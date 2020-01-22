package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.data.util.Lazy;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.ihsmarkit.tfx.core.dl.entity.custodianaccount.CustodianAccountEntity;
import com.ihsmarkit.tfx.core.domain.type.CollateralPurpose;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AbstractCustodianAccountCodeProvider {

    private final Lazy<Table<String, CollateralPurpose, String>> codes;

    public <T extends CustodianAccountEntity> AbstractCustodianAccountCodeProvider(
        final Supplier<List<T>> accountsSupplier,
        final Function<T, String> codeMapping
    ) {
        codes = Lazy.of(() ->
            accountsSupplier.get().stream()
                .collect(Tables.toTable(
                    account -> account.getParticipant().getCode(),
                    CustodianAccountEntity::getPurpose,
                    codeMapping,
                    HashBasedTable::create
                )));
    }

    public Optional<String> getCode(final String participantCode, final CollateralPurpose purpose) {
        try {
            return Optional.ofNullable(codes.get().get(participantCode, purpose));
        } catch (final Exception ex) {
            log.error("error while retrieving custodian account information for participantCode: {} and purpose: {} with message: {}",
                participantCode, purpose, ex.getMessage());
            return Optional.empty();
        }
    }

}
