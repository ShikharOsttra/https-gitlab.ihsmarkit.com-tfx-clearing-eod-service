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
import com.ihsmarkit.tfx.eod.mtm.DailySettlementPriceRegistry;
import com.ihsmarkit.tfx.eod.mtm.TradeMTM;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@JobScope
public class MarkToMarketTradesTasklet implements Tasklet {

    public static final Map<CurrencyPairEntity, BigDecimal> PRICE_MAP = Map.of(
            CurrencyPairEntity.of(0L, "USD", "JPY"), new BigDecimal(99.111d),
            CurrencyPairEntity.of(1L, "EUR", "JPY"), new BigDecimal(120.79),
            CurrencyPairEntity.of(2L, "EUR", "USD"), new BigDecimal(1.1d),
            CurrencyPairEntity.of(81L, "USD", "EUR"), new BigDecimal(0.9d)
    );

    @Autowired
    private final TradeRepository tradeRepository;

    @Autowired
    private final EodProductCashSettlementRepository eodProductCashSettlementRepository;

    @Autowired
    private final ParticipantPositionRepository participantPositionRepository;

    @Value("#{jobParameters['businessDate']}")
    private String businessDateStr;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) throws Exception {
        final LocalDate businessDate = LocalDate.parse(businessDateStr, BUSINESS_DATE_FMT);

        DailySettlementPriceRegistry dsp = new DailySettlementPriceRegistry(PRICE_MAP);

        Stream<TradeEntity> novatedTrades = tradeRepository.findAllNovatedForTradeDate(businessDate);
        final Stream<EodProductCashSettlementEntity> initial = dsp.calculateAndAggregateInitialMTM(novatedTrades)
                .map(a -> mapToEodProductCashSettlementEntity(a, businessDate, EodProductCashSettlementType.INITIAL_MTM));
        eodProductCashSettlementRepository.saveAll(initial::iterator);

        Collection<ParticipantPositionEntity> positions =
                participantPositionRepository.findAllByPositionTypeAndTradeDateFetchCurrencyPair(ParticipantPositionType.SOD, businessDate);

        final Stream<EodProductCashSettlementEntity> daily = dsp.calculateAndAggregateDailyMTM(positions)
                .map(a -> mapToEodProductCashSettlementEntity(a, businessDate, EodProductCashSettlementType.DAILY_MTM));

        eodProductCashSettlementRepository.saveAll(daily::iterator);

        return RepeatStatus.FINISHED;
    }

    private EodProductCashSettlementEntity mapToEodProductCashSettlementEntity(TradeMTM mtm, LocalDate businessDate, EodProductCashSettlementType type) {
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
