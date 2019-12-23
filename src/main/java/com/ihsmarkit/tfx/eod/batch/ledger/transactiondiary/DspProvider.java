package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Lazy;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.marketdata.DailySettlementPriceEntity;
import com.ihsmarkit.tfx.core.dl.repository.marketdata.DailySettlementPriceRepository;

import lombok.Getter;

@Service
@StepScope
public class DspProvider {

    @Getter
    private final Lazy<Map<String, DailySettlementPriceEntity>> dsp;

    public DspProvider(
        final DailySettlementPriceRepository dspRepository, @Value("#{jobParameters['businessDate']}") final LocalDate businessDate) {

        dsp = Lazy.of(() -> dspRepository.findAllByBusinessDate(businessDate)
            .stream()
            .collect(Collectors.toMap(dsp -> dsp.getCurrencyPair().getCode(), Function.identity())));
    }

    Optional<DailySettlementPriceEntity> getDsp(final String currencyPairCode) {
        return Optional.ofNullable(dsp.get().get(currencyPairCode));
    }
}
