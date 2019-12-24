package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDate;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDateTime;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatEnum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.entity.marketdata.DailySettlementPriceEntity;
import com.ihsmarkit.tfx.eod.model.ledger.TransactionDiary;
import com.ihsmarkit.tfx.eod.service.CurrencyPairSwapPointService;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceService;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.JPYRateService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@StepScope
public class SODTransactionDiaryLedgerProcessor implements TransactionDiaryLedgerProcessor<ParticipantPositionEntity> {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Value("#{stepExecutionContext['recordDate']}")
    private final LocalDateTime recordDate;

    private final EODCalculator eodCalculator;
    private final CurrencyPairSwapPointService currencyPairSwapPointService;
    private final JPYRateService jpyRateService;
    private final DailySettlementPriceService dailySettlementPriceService;
    private final DspProvider dspProvider;

    @Override
    public TransactionDiary process(final ParticipantPositionEntity participantPosition) {
        final Function<CurrencyPairEntity, BigDecimal> dspResolver = ccy -> dailySettlementPriceService.getPrice(businessDate, ccy);
        final Function<String, BigDecimal> jpyRatesResolver = ccy -> jpyRateService.getJpyRate(businessDate, ccy);
        final Function<CurrencyPairEntity, BigDecimal> swapPointResolver = ccy -> currencyPairSwapPointService.getSwapPoint(businessDate, ccy);

        final ParticipantEntity participant = participantPosition.getParticipant();
        final Optional<DailySettlementPriceEntity> dailySettlementPriceEntity =
            dspProvider.getDspByCurrencyCode(participantPosition.getCurrencyPair().getCode());
        final String dailyMtMAmount = eodCalculator.calculateDailyMtmValue(participantPosition, dspResolver, jpyRatesResolver).getAmount().toString();
        final String swapPoint = eodCalculator.calculateSwapPoint(participantPosition, swapPointResolver, jpyRatesResolver).getAmount().toString();
        final String settlementDate = formatDate(participantPosition.getValueDate());

        @Nullable
        final String dsp = dailySettlementPriceEntity.map(dspEntity -> dspEntity.getDailySettlementPrice().toString()).orElse(null);

        //todo: are rest of the fields empty??
        return TransactionDiary.builder()
            .businessDate(businessDate)
            .tradeDate(formatDate(businessDate))
            .recordDate(formatDateTime(recordDate))
            .participantCode(participant.getCode())
            .participantName(participant.getName())
            .participantType(formatEnum(participant.getType()))
            //todo ???
            .currencyNo(participantPosition.getCurrencyPair().getCode())
            .currencyPair(participantPosition.getCurrencyPair().getCode())
            //todo is it correct
            .tradePrice(dsp)
            .dsp(dsp)
            .dailyMtMAmount(dailyMtMAmount)
            .swapPoint(swapPoint)
            .settlementDate(settlementDate)
            .userReference(null)
            .build();
    }
}
