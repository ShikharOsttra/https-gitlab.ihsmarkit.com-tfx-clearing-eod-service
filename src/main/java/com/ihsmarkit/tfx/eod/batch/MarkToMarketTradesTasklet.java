package com.ihsmarkit.tfx.eod.batch;

import static com.ihsmarkit.tfx.eod.config.SpringBatchConfig.BUSINESS_DATE_FMT;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodProductCashSettlementRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.eod.model.MarkToMarketTrade;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceProvider;
import com.ihsmarkit.tfx.eod.service.TradeMtmCalculator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@JobScope
public class MarkToMarketTradesTasklet implements Tasklet {

    @Autowired
    private final TradeRepository tradeRepository;

    @Autowired
    private final EodProductCashSettlementRepository eodProductCashSettlementRepository;

    @Autowired
    private final ParticipantPositionRepository participantPositionRepository;

    @Autowired
    private final DailySettlementPriceProvider dailySettlementPriceProvider;

    @Autowired
    private final TradeMtmCalculator tradeMtmCalculator;

    @Value("#{jobParameters['businessDate']}")
    private String businessDateStr;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) {
        final LocalDate businessDate = LocalDate.parse(businessDateStr, BUSINESS_DATE_FMT);

        final Map<CurrencyPairEntity, BigDecimal> dsp = dailySettlementPriceProvider.getDailySettlementPrices(businessDate);

        final Stream<TradeEntity> novatedTrades = tradeRepository.findAllNovatedForTradeDate(businessDate);
        final Stream<EodProductCashSettlementEntity> initial = tradeMtmCalculator.calculateAndAggregateInitialMtm(novatedTrades, dsp)
            .map(a -> mapToEodProductCashSettlementEntity(a, businessDate, EodProductCashSettlementType.INITIAL_MTM));
        eodProductCashSettlementRepository.saveAll(initial::iterator);

        final Collection<ParticipantPositionEntity> positions =
            participantPositionRepository.findAllByPositionTypeAndTradeDateFetchCurrencyPair(ParticipantPositionType.SOD, businessDate);

        final Stream<EodProductCashSettlementEntity> daily = tradeMtmCalculator.calculateAndAggregateDailyMtm(positions, dsp)
            .map(a -> mapToEodProductCashSettlementEntity(a, businessDate, EodProductCashSettlementType.DAILY_MTM));

        eodProductCashSettlementRepository.saveAll(daily::iterator);

        return RepeatStatus.FINISHED;
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private EodProductCashSettlementEntity mapToEodProductCashSettlementEntity(final MarkToMarketTrade mtm,
        final LocalDate businessDate, final EodProductCashSettlementType type) {

        return EodProductCashSettlementEntity.builder()
            .type(type)
            .participant(mtm.getParticipant())
            .currencyPair(mtm.getCurrencyPair())
            .amount(AmountEntity.of(mtm.getAmount(), "JPY"))
            .date(businessDate)
            .settlementDate(businessDate.plusDays(3))
            .build();
    }
}
