package com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume;

import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.MONTHLY_VOLUME_NET_BUY;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.MONTHLY_VOLUME_NET_SELL;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.ITEM_RECORD_TYPE;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDate;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDateTime;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatEnum;
import static com.ihsmarkit.tfx.eod.service.EODCalculator.twoWayCollector;
import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.eod.batch.ledger.ParticipantCodeOrderIdProvider;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;
import com.ihsmarkit.tfx.eod.model.ledger.MonthlyTradingVolumeItem;
import com.ihsmarkit.tfx.eod.service.FXSpotProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class MonthlyTradingVolumeProcessor implements ItemProcessor<ParticipantAndCurrencyPair, MonthlyTradingVolumeItem<BigDecimal>> {

    private static final Set<ParticipantPositionType> MONTHLY_TRADING_VOLUME_POSITION_TYPES = Set.of(
        MONTHLY_VOLUME_NET_BUY,
        MONTHLY_VOLUME_NET_SELL
    );

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Value("#{stepExecutionContext['recordDate']}")
    private final LocalDateTime recordDate;

    private final FXSpotProductService fxSpotProductService;

    private final ParticipantPositionRepository participantPositionRepository;

    private final ParticipantCodeOrderIdProvider participantCodeOrderIdProvider;

    @Override
    public MonthlyTradingVolumeItem<BigDecimal> process(final ParticipantAndCurrencyPair item) {
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
                (buyAmount, sellAmount) -> mapToTradingVolumeModel(item, buyAmount, sellAmount)
            )
        );
    }

    private MonthlyTradingVolumeItem<BigDecimal> mapToTradingVolumeModel(
        final ParticipantAndCurrencyPair item,
        final BigDecimal buyAmount,
        final BigDecimal sellAmount
    ) {
        final ParticipantEntity participant = item.getParticipant();
        final var currencyPair = item.getCurrencyPair();
        final var tradingUnit = fxSpotProductService.getFxSpotProduct(currencyPair).getTradingUnit();
        final var productNumber = fxSpotProductService.getFxSpotProduct(currencyPair).getProductNumber();
        final var participantCode = participant.getCode();

        return MonthlyTradingVolumeItem.<BigDecimal>builder()
            .businessDate(businessDate.with(firstDayOfMonth()))
            .tradeDate(formatDate(businessDate))
            .recordDate(formatDateTime(recordDate))
            .participantCode(participantCode)
            .participantName(participant.getName())
            .participantType(formatEnum(participant.getType()))
            .currencyPairCode(currencyPair.getCode())
            .currencyPairNumber(productNumber)
            .sellTradingVolumeInUnit(amountInUnit(sellAmount, tradingUnit))
            .buyTradingVolumeInUnit(amountInUnit(buyAmount, tradingUnit))

            .orderId(getOrderId(participantCode, productNumber))
            .recordType(ITEM_RECORD_TYPE)
            .build();
    }

    private long getOrderId(final String participantCode, final String productNumber) {
        return Long.parseLong(participantCodeOrderIdProvider.get(participantCode) + productNumber);
    }

    private BigDecimal amountInUnit(final BigDecimal amount, final Long tradingUnit) {
        return amount.divide(BigDecimal.valueOf(tradingUnit), 0, RoundingMode.DOWN);
    }
}
