package com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume;

import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.MONTHLY_VOLUME_NET_BUY;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.MONTHLY_VOLUME_NET_SELL;
import static com.ihsmarkit.tfx.eod.service.EODCalculator.twoWayCollector;
import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Set;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume.domain.MonthlyTradingVolumeItem;
import com.ihsmarkit.tfx.eod.model.BuySellAmounts;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;
import com.ihsmarkit.tfx.eod.service.FXSpotProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class MonthlyTradingVolumeInputProcessor implements ItemProcessor<ParticipantAndCurrencyPair, MonthlyTradingVolumeItem> {

    private static final Set<ParticipantPositionType> MONTHLY_TRADING_VOLUME_POSITION_TYPES = Set.of(
        MONTHLY_VOLUME_NET_BUY,
        MONTHLY_VOLUME_NET_SELL
    );

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    private final ParticipantPositionRepository participantPositionRepository;

    private final FXSpotProductService fxSpotProductService;


    @Override
    public MonthlyTradingVolumeItem process(final ParticipantAndCurrencyPair item) {
        return participantPositionRepository.findAllByParticipantAndCurrencyPairAndTypeInAndTradeDateBetween(
            item.getParticipant(),
            item.getCurrencyPair(),
            MONTHLY_TRADING_VOLUME_POSITION_TYPES,
            businessDate.with(firstDayOfMonth()),
            businessDate.with(lastDayOfMonth())
        ).collect(
            twoWayCollector(
                position -> position.getType() == MONTHLY_VOLUME_NET_BUY,
                position -> position.getAmount().getValue().abs(),
                (buyAmount, sellAmount) -> MonthlyTradingVolumeItem.builder()
                    .participantAndCurrencyPair(item)
                    .buySellAmounts(BuySellAmounts.of(amountInUnit(buyAmount, item), amountInUnit(sellAmount, item)))
                    .build()
            )
        );
    }

    private BigDecimal amountInUnit(final BigDecimal amount, final ParticipantAndCurrencyPair participantAndCurrencyPair) {
        final var tradingUnit = fxSpotProductService.getFxSpotProduct(participantAndCurrencyPair.getCurrencyPair()).getTradingUnit();
        return amount.divide(BigDecimal.valueOf(tradingUnit), 0, RoundingMode.DOWN);
    }
}
