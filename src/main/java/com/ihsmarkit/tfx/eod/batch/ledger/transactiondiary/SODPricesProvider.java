package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Lazy;
import org.springframework.stereotype.Service;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;

@Service
@JobScope
public class SODPricesProvider {

    private final Lazy<Table<String, String, String>> prices;

    public SODPricesProvider(@Value("#{jobParameters['businessDate']}") final LocalDate businessDate,
        final ParticipantPositionRepository participantPositionRepository) {
        prices = Lazy.of(() -> participantPositionRepository.findAllByPositionTypeAndTradeDateFetchCurrencyPair(ParticipantPositionType.SOD, businessDate)
            .stream()
            .collect(Tables.toTable(
                participantPosition -> participantPosition.getParticipant().getCode(),
                participantPosition -> participantPosition.getCurrencyPair().getCode(),
                participantPosition -> participantPosition.getPrice().toString(),
                HashBasedTable::create
            )));
    }

    public Optional<String> getPrice(final String participantCode, final String currencyPairCode) {
        return Optional.ofNullable(prices.get().get(participantCode, currencyPairCode));
    }
}
