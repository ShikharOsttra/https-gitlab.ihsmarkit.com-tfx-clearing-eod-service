package com.ihsmarkit.tfx.eod.batch.ledger.openpositions;

import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.DAILY_MTM;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.INITIAL_MTM;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.SWAP_PNL;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.TOTAL_VM;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.NET;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.REBALANCING;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.SOD;
import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodProductCashSettlementRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;
import com.ihsmarkit.tfx.eod.model.ledger.OpenPositionsListItem;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@StepScope
public class OpenPositionsLedgerProcessor implements ItemProcessor<ParticipantAndCurrencyPair, OpenPositionsListItem> {
    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    private final ParticipantPositionRepository participantPositionRepository;

    private final EodProductCashSettlementRepository eodProductCashSettlementRepository;

    @Override
    public OpenPositionsListItem process(ParticipantAndCurrencyPair item) throws Exception {

        final Map<ParticipantPositionType, ParticipantPositionEntity> positions =
            participantPositionRepository.findAllByParticipantAndCurrencyPairAndTradeDate(item.getParticipant(), item.getCurrencyPair(), businessDate)
                .collect(Collectors.toMap(ParticipantPositionEntity::getType, Function.identity()));

        final Map<EodProductCashSettlementType, EodProductCashSettlementEntity> margins =
            eodProductCashSettlementRepository.findAllByParticipantAndCurrencyPairAndDate(item.getParticipant(), item.getCurrencyPair(), businessDate)
                .collect(Collectors.toMap(EodProductCashSettlementEntity::getType, Function.identity()));

        return OpenPositionsListItem.builder()
            .participantCode(item.getParticipant().getCode())
            .participantName(item.getParticipant().getName())
            .participantType(item.getParticipant().getType().toString())
            .currencyCode(item.getCurrencyPair().getCode())
            //.currencyNo(item.getCurrencyPair().)
            .tradeDate(businessDate.toString())
            .shortPositionPreviousDay(format(shortPosition(getPosition(positions, SOD))))
            .longPositionPreviousDay(format(longPosition(getPosition(positions, SOD))))
            .shortPosition(format(shortPosition(getPositionsSummed(positions, NET, REBALANCING))))
            .longPosition(format(longPosition(getPositionsSummed(positions, NET, REBALANCING))))
            .initialMtmAmount(format(getMargin(margins, INITIAL_MTM)))
            .dailyMtmAmount(format(getMargin(margins, DAILY_MTM)))
            .swapPoint(format(getMargin(margins, SWAP_PNL)))
            .totalVariationMargin(format(getMargin(margins, TOTAL_VM)))

            .build();
    }

    private String format(Optional<BigDecimal> value) {
        return value.orElse(ZERO).toString();
    }

    private Optional<BigDecimal> longPosition(Optional<BigDecimal> amount) {
        return amount.map(ZERO::max);
    }

    private Optional<BigDecimal> shortPosition(Optional<BigDecimal> amount) {
        return amount.map(ZERO::min).map(BigDecimal::negate);
    }

    private Optional<BigDecimal> getMargin(Map<EodProductCashSettlementType, EodProductCashSettlementEntity> margins, EodProductCashSettlementType initialMtm) {
        return Optional.ofNullable(margins.get(initialMtm)).map(EodProductCashSettlementEntity::getAmount).map(AmountEntity::getValue);
    }

    private Optional<BigDecimal> getPosition(Map<ParticipantPositionType, ParticipantPositionEntity> positions, ParticipantPositionType type) {
        return Optional.ofNullable(positions.get(type))
            .map(ParticipantPositionEntity::getAmount)
            .map(AmountEntity::getValue);
    }

    private Optional<BigDecimal> getPositionsSummed(Map<ParticipantPositionType, ParticipantPositionEntity> positions, ParticipantPositionType...type) {
        return Arrays.stream(type)
            .map(positions::get)
            .map(Optional::ofNullable)
            .flatMap(Optional::stream)
            .map(ParticipantPositionEntity::getAmount)
            .map(AmountEntity::getValue)
            .reduce(BigDecimal::add);
    }

}
