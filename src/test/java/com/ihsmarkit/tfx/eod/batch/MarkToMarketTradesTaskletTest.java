package com.ihsmarkit.tfx.eod.batch;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aCurrencyPairEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aParticipantEntityBuilder;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.SOD;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_FMT;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.JPY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodProductCashSettlementRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;
import com.ihsmarkit.tfx.eod.mapper.TradeOrPositionEssentialsMapper;
import com.ihsmarkit.tfx.eod.model.MarkToMarketTrade;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceProvider;
import com.ihsmarkit.tfx.eod.service.TradeMtmCalculator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

class MarkToMarketTradesTaskletTest extends AbstractSpringBatchTest {

    private static final CurrencyPairEntity CURRENCY_PAIR_USD = aCurrencyPairEntityBuilder().build();
    private static final CurrencyPairEntity CURRENCY_PAIR_JPY = aCurrencyPairEntityBuilder().baseCurrency(JPY).build();
    private static final ParticipantEntity PARTICIPANT = aParticipantEntityBuilder().build();

    @MockBean
    private TradeRepository tradeRepository;

    @MockBean
    private DailySettlementPriceProvider dailySettlementPriceProvider;

    @MockBean
    private EodProductCashSettlementRepository eodProductCashSettlementRepository;

    @MockBean
    private ParticipantPositionRepository participantPositionRepository;

    @MockBean
    private TradeMtmCalculator tradeMtmCalculator;

    @Captor
    private ArgumentCaptor<Iterable<EodProductCashSettlementEntity>> captor;

    @Mock
    private Stream<TradeEntity> trades;

    @Mock
    private Collection<ParticipantPositionEntity> positions;

    @Mock
    private Map<CurrencyPairEntity, BigDecimal> dailySettlementPrices;

    @Test
    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    void shouldCalculateAndStoreDailyAndInitialMtm() {

        final String businessDateStr = "20191006";
        final LocalDate businessDate = LocalDate.parse(businessDateStr, BUSINESS_DATE_FMT);

        when(tradeRepository.findAllNovatedForTradeDate(any())).thenReturn(trades);

        when(participantPositionRepository.findAllByPositionTypeAndTradeDateFetchCurrencyPair(any(), any()))
            .thenReturn(positions);
        when(dailySettlementPriceProvider.getDailySettlementPrices(businessDate))
            .thenReturn(dailySettlementPrices);

        when(tradeMtmCalculator.calculateAndAggregateInitialMtm(any(), any()))
            .thenReturn(
                Stream.of(
                    MarkToMarketTrade.of(PARTICIPANT, CURRENCY_PAIR_USD, BigDecimal.ONE),
                    MarkToMarketTrade.of(PARTICIPANT, CURRENCY_PAIR_JPY, BigDecimal.valueOf(2))
                )
            );

        when(tradeMtmCalculator.calculateAndAggregateDailyMtm(any(), any()))
            .thenReturn(Stream.of(MarkToMarketTrade.of(PARTICIPANT, CURRENCY_PAIR_USD, BigDecimal.TEN)));

        final JobExecution execution = jobLauncherTestUtils.launchStep("mtmTrades",
            new JobParametersBuilder().addString("businessDate", businessDateStr).toJobParameters());
        assertThat(execution.getStatus()).isSameAs(BatchStatus.COMPLETED);

        verify(tradeMtmCalculator).calculateAndAggregateDailyMtm(positions, dailySettlementPrices);
        verify(tradeMtmCalculator).calculateAndAggregateInitialMtm(trades, dailySettlementPrices);

        verify(dailySettlementPriceProvider).getDailySettlementPrices(businessDate);
        verify(tradeRepository).findAllNovatedForTradeDate(businessDate);
        verify(participantPositionRepository).findAllByPositionTypeAndTradeDateFetchCurrencyPair(SOD, businessDate);

        verify(eodProductCashSettlementRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
            .extracting(
                EodProductCashSettlementEntity::getParticipant,
                EodProductCashSettlementEntity::getCurrencyPair,
                EodProductCashSettlementEntity::getType,
                EodProductCashSettlementEntity::getAmount,
                EodProductCashSettlementEntity::getDate,
                EodProductCashSettlementEntity::getSettlementDate
            )
            .containsExactlyInAnyOrder(
                Tuple.tuple(
                    PARTICIPANT,
                    CURRENCY_PAIR_USD,
                    EodProductCashSettlementType.DAILY_MTM, AmountEntity.of(BigDecimal.TEN, JPY),
                    businessDate,
                    LocalDate.of(2019, 10, 9)
                ),
                Tuple.tuple(
                    PARTICIPANT,
                    CURRENCY_PAIR_USD,
                    EodProductCashSettlementType.INITIAL_MTM, AmountEntity.of(BigDecimal.ONE, JPY),
                    businessDate,
                    LocalDate.of(2019, 10, 9)
                ),
                Tuple.tuple(
                    PARTICIPANT,
                    CURRENCY_PAIR_JPY,
                    EodProductCashSettlementType.INITIAL_MTM, AmountEntity.of(BigDecimal.valueOf(2), JPY),
                    businessDate,
                    LocalDate.of(2019, 10, 9)
                )
            );

        verifyNoMoreInteractions(
            tradeRepository,
            participantPositionRepository,
            dailySettlementPriceProvider,
            tradeMtmCalculator,
            eodProductCashSettlementRepository
        );
    }

    @TestConfiguration
    @ComponentScan(basePackageClasses = {
        MarkToMarketTradesTasklet.class, TradeOrPositionEssentialsMapper.class, EodProductCashSettlementRepository.class,
        TradeMtmCalculator.class
    },
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
            classes = {
                MarkToMarketTradesTasklet.class,
                TradeMtmCalculator.class,
                TradeOrPositionEssentialsMapper.class,
                EodProductCashSettlementRepository.class
        })
    )
    static class TestConfig {

    }
}