package com.ihsmarkit.tfx.eod.batch.ledger.marketdata;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Lazy;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;

import lombok.RequiredArgsConstructor;


@Component
@JobScope
@RequiredArgsConstructor
public class OffsettedTradeMatchIdProvider {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    private final TradeRepository tradeRepository;

    private final Lazy<Set<String>> offsettingMatchIds = Lazy.of(this::loadOffsettingMatchIds);

    public Set<String> getOffsettingMatchIds() {
        return offsettingMatchIds.get();
    }

    private Set<String> loadOffsettingMatchIds() {
        return tradeRepository.findAllOffsettingMatchIdsByTradeDate(businessDate);
    }

}
