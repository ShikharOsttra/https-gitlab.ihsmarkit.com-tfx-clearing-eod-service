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
        return Optional.ofNullable(codes.get().get(participantCode, purpose));
    }

}
