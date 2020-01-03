package com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume;

import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.BUY;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.SELL;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatBigDecimal;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDate;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDateTime;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatEnum;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.eod.model.ledger.MonthlyTradingVolumeItem;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class MonthlyTradingVolumeProcessor implements ItemProcessor<List<ParticipantPositionEntity>, List<MonthlyTradingVolumeItem>> {

    private final FxSpotProductDataProvider fxSpotProductDataProvider;
    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;
    @Value("#{stepExecutionContext['recordDate']}")
    private final LocalDateTime recordDate;

    @Override
    public List<MonthlyTradingVolumeItem> process(final List<ParticipantPositionEntity> item) {
        return item.stream()
            .collect(Collectors.groupingBy(ParticipantPositionKey::new))
            .entrySet().stream()
            .map(this::mapToTradingVolumeModel)
            .collect(Collectors.toUnmodifiableList());
    }

    private MonthlyTradingVolumeItem mapToTradingVolumeModel(final Map.Entry<ParticipantPositionKey, List<ParticipantPositionEntity>> positionEntry) {
        log.info("Building monthly trading volume for participant & currency pair: {}", positionEntry.getKey());

        final List<ParticipantPositionEntity> positions = positionEntry.getValue();
        final ParticipantEntity participant = positions.get(0).getParticipant();
        final String currencyPairCode = positionEntry.getKey().getCurrencyPairCode();
        final BigDecimal tradingUnit = fxSpotProductDataProvider.getTradingUnit(currencyPairCode);

        // todo: math context?
        final BigDecimal sellTradingPositionInUnit = getParticipantPositionVolumeBySide(positions, SELL)
            .abs()
            .divide(tradingUnit);
        final BigDecimal buyTradingPositionInUnit = getParticipantPositionVolumeBySide(positions, BUY)
            .abs()
            .divide(tradingUnit);

        return MonthlyTradingVolumeItem.builder()
            .businessDate(Date.valueOf(businessDate))
            .tradeDate(formatDate(businessDate))
            .recordDate(formatDateTime(recordDate))
            .participantCode(participant.getCode())
            .participantName(participant.getName())
            .participantType(formatEnum(participant.getType()))
            .currencyPairCode(currencyPairCode)
            .currencyPairNumber(fxSpotProductDataProvider.getProductNumber(currencyPairCode))
            .sellTradingVolumeInUnit(formatBigDecimal(sellTradingPositionInUnit))
            .buyTradingVolumeInUnit(formatBigDecimal(buyTradingPositionInUnit))
            .build();
    }

    private static BigDecimal getParticipantPositionVolumeBySide(final List<ParticipantPositionEntity> positions, final ParticipantPositionType positionType) {
        return positions.stream()
            .filter(item -> item.getType() == positionType)
            .findFirst()
            .map(ParticipantPositionEntity::getAmount)
            .map(AmountEntity::getValue)
            // todo: what is a default value if no position?
            .orElse(BigDecimal.ZERO);
    }

    @ToString
    @Getter
    @EqualsAndHashCode
    private static final class ParticipantPositionKey {
        private final String currencyPairCode;
        private final String participantCode;

        ParticipantPositionKey(final ParticipantPositionEntity position) {
            this.currencyPairCode = position.getCurrencyPair().getCode();
            this.participantCode = position.getParticipant().getCode();
        }
    }
}
