package com.ihsmarkit.tfx.eod.batch;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.core.domain.type.TransactionType;
import com.ihsmarkit.tfx.eod.batch.ledger.marketdata.OffsettedTradeMatchIdProvider;
import com.ihsmarkit.tfx.eod.mapper.ParticipantCurrencyPairAmountMapper;
import com.ihsmarkit.tfx.eod.mapper.TradeOrPositionEssentialsMapper;
import com.ihsmarkit.tfx.eod.model.ParticipantPosition;
import com.ihsmarkit.tfx.eod.model.TradeOrPositionEssentials;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceService;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.TradeAndSettlementDateService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@JobScope
public class NettingTasklet implements Tasklet {

    private static final Set<TransactionType> MONTHLY_VOLUME_TRANSACTION_TYPES = Set.of(
        TransactionType.REGULAR,
        TransactionType.FORCE_ALLOCATION,
        TransactionType.NEW_TRANSACTION_BY_CLHS,
        TransactionType.POSITION_OFFSET
    );

    private final TradeRepository tradeRepository;

    private final ParticipantPositionRepository participantPositionRepository;

    private final DailySettlementPriceService dailySettlementPriceService;

    private final EODCalculator eodCalculator;

    private final TradeAndSettlementDateService tradeAndSettlementDateService;

    private final ParticipantCurrencyPairAmountMapper participantCurrencyPairAmountMapper;

    private final TradeOrPositionEssentialsMapper tradeOrPositionMapper;

    private final OffsettedTradeMatchIdProvider offsettedTradeMatchIdProvider;

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) {

        final Stream<TradeEntity> novatedTrades = tradeRepository.findAllNovatedForTradeDate(businessDate);

        final Map<Boolean, List<TradeOrPositionEssentials>> allNovatedTradesGroupedByScope = novatedTrades
            .collect(partitioningBy(this::isInScopeOfMonthlyVolumeReport, mapping(tradeOrPositionMapper::convertTrade, toList())));

        final List<TradeOrPositionEssentials> tradesInScopeOfMonthlyVolumeReport = allNovatedTradesGroupedByScope.getOrDefault(Boolean.TRUE, List.of());

        final Stream<ParticipantPositionEntity> sodParticipantPositions =
            participantPositionRepository.findAllByPositionTypeAndTradeDateFetchCurrencyPair(ParticipantPositionType.SOD, businessDate);

        final Stream<TradeOrPositionEssentials> sodPositions = sodParticipantPositions
            .map(tradeOrPositionMapper::convertPosition);

        final Stream<ParticipantPositionEntity> netted =
            Stream.concat(
                eodCalculator.netAllByBuySell(
                    allNovatedTradesGroupedByScope.values().stream()
                        .flatMap(Collection::stream),
                    sodPositions
                ),
                eodCalculator.netByBuySellForMonthlyVolumeReport(tradesInScopeOfMonthlyVolumeReport.stream())
            )
                .map(this::mapParticipantPositionEntity);


        participantPositionRepository.saveAll(netted::iterator);

        return RepeatStatus.FINISHED;
    }

    private ParticipantPositionEntity mapParticipantPositionEntity(final ParticipantPosition participantPosition) {
        final CurrencyPairEntity currencyPair = participantPosition.getCurrencyPair();
        final LocalDate settlementDate = tradeAndSettlementDateService.getValueDate(businessDate, currencyPair);
        final BigDecimal dspPrice = dailySettlementPriceService.getPrice(businessDate, currencyPair);
        return participantCurrencyPairAmountMapper.toParticipantPosition(participantPosition, businessDate, settlementDate, dspPrice);
    }

    private boolean isInScopeOfMonthlyVolumeReport(final TradeEntity tradeEntity) {
        return MONTHLY_VOLUME_TRANSACTION_TYPES.contains(tradeEntity.getTransactionType())
            && !isCancelledByOboTransaction(tradeEntity);
    }

    private boolean isCancelledByOboTransaction(final TradeEntity tradeEntity) {
        return offsettedTradeMatchIdProvider.hasOffsettingMatchId(tradeEntity.getMatchingRef());
    }
}
