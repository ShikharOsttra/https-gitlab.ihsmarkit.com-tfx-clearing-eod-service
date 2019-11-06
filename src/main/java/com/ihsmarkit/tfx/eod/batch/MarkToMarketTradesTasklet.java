package com.ihsmarkit.tfx.eod.batch;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_FMT;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
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
import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodProductCashSettlementRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.eod.mapper.ParticipantPositionForPairMapper;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceProvider;
import com.ihsmarkit.tfx.eod.service.SettlementDateProvider;
import com.ihsmarkit.tfx.eod.service.TradeMtmCalculator;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@JobScope
public class MarkToMarketTradesTasklet implements Tasklet {

    private final TradeRepository tradeRepository;

    private final EodProductCashSettlementRepository eodProductCashSettlementRepository;

    private final ParticipantPositionRepository participantPositionRepository;

    private final DailySettlementPriceProvider dailySettlementPriceProvider;

    private final TradeMtmCalculator tradeMtmCalculator;

    private final ParticipantPositionForPairMapper mtmMapper;

    private final SettlementDateProvider settlementDateProvider;

    @Value("#{jobParameters['businessDate']}")
    private final String businessDateJobParameter;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) {
        final LocalDate businessDate = LocalDate.parse(businessDateJobParameter, BUSINESS_DATE_FMT);
        final LocalDate settlementDate = settlementDateProvider.getSettlementDateFor(businessDate);

        final Map<CurrencyPairEntity, BigDecimal> dsp = dailySettlementPriceProvider.getDailySettlementPrices(businessDate);

        final Stream<TradeEntity> novatedTrades = tradeRepository.findAllNovatedForTradeDate(businessDate);
        final Stream<EodProductCashSettlementEntity> initial = tradeMtmCalculator.calculateAndAggregateInitialMtm(novatedTrades, dsp)
            .map(mtm -> mtmMapper.toEodProductCashSettlement(mtm, businessDate, settlementDate, EodProductCashSettlementType.INITIAL_MTM));

        final Collection<ParticipantPositionEntity> positions =
            participantPositionRepository.findAllByPositionTypeAndTradeDateFetchCurrencyPair(ParticipantPositionType.SOD, businessDate);

        final Stream<EodProductCashSettlementEntity> daily = tradeMtmCalculator.calculateAndAggregateDailyMtm(positions, dsp)
            .map(mtm -> mtmMapper.toEodProductCashSettlement(mtm, businessDate, settlementDate, EodProductCashSettlementType.DAILY_MTM));

        eodProductCashSettlementRepository.saveAll(Stream.concat(initial, daily)::iterator);

        return RepeatStatus.FINISHED;
    }

}
