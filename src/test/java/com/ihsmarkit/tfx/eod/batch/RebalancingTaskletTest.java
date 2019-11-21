package com.ihsmarkit.tfx.eod.batch;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aParticipantEntityBuilder;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_FMT;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_JOB_PARAM_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.REBALANCE_POSITIONS_STEP_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import com.ihsmarkit.tfx.core.dl.EntityTestDataFactory;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.LegalEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.core.domain.type.Side;
import com.ihsmarkit.tfx.eod.mapper.TradeOrPositionEssentialsMapper;
import com.ihsmarkit.tfx.eod.model.BalanceTrade;
import com.ihsmarkit.tfx.eod.model.CcyParticipantAmount;
import com.ihsmarkit.tfx.eod.model.ParticipantPositionForPair;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceProvider;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.SettlementDateProvider;

class RebalancingTaskletTest extends AbstractSpringBatchTest {

    private static final CurrencyPairEntity EURUSD = CurrencyPairEntity.of(2L, "EUR", "USD");
    private static final BigDecimal EURUSD_RATE = BigDecimal.valueOf(1.1);

    private static final LegalEntity ORIG_A = EntityTestDataFactory.aLegalEntityBuilder().name("LEA").build();
    private static final LegalEntity ORIG_B = EntityTestDataFactory.aLegalEntityBuilder().name("LEB").build();
    private static final LegalEntity ORIG_C = EntityTestDataFactory.aLegalEntityBuilder().name("LEC").build();
    private static final LegalEntity ORIG_D = EntityTestDataFactory.aLegalEntityBuilder().name("LED").build();

    private static final ParticipantEntity PARTICIPANT_A = aParticipantEntityBuilder().legalEntities(List.of(ORIG_A)).name("A").build();
    private static final ParticipantEntity PARTICIPANT_B = aParticipantEntityBuilder().legalEntities(List.of(ORIG_B)).name("B").build();
    private static final ParticipantEntity PARTICIPANT_C = aParticipantEntityBuilder().legalEntities(List.of(ORIG_C)).name("C").build();
    private static final ParticipantEntity PARTICIPANT_D = aParticipantEntityBuilder().legalEntities(List.of(ORIG_D)).name("D").build();

    @MockBean
    private TradeRepository tradeRepository;

    @MockBean
    private ParticipantPositionRepository participantPositionRepository;

    @MockBean
    private DailySettlementPriceProvider dailySettlementPriceProvider;

    @MockBean
    private EODCalculator eodCalculator;

    @MockBean
    private SettlementDateProvider settlementDateProvider;

    @Captor
    private ArgumentCaptor<Iterable<ParticipantPositionEntity>> positionCaptor;

    @Captor
    private ArgumentCaptor<Iterable<TradeEntity>> tradeCaptor;

    @Captor
    private ArgumentCaptor<Stream<CcyParticipantAmount>> netCaptor;

    @Test
    void shouldRebalanceTrades() {
        final String businessDateStr = "20191006";
        final LocalDate businessDate = LocalDate.parse(businessDateStr, BUSINESS_DATE_FMT);
        final LocalDate valueDate = businessDate.plusDays(2);

        List<ParticipantPositionEntity> positions = Collections.emptyList();

        when(settlementDateProvider.getSettlementDateFor(businessDate)).thenReturn(valueDate);

        when(dailySettlementPriceProvider.getDailySettlementPrices(businessDate))
            .thenReturn(Collections.singletonMap(EURUSD, EURUSD_RATE));

        when(participantPositionRepository.findAllByPositionTypeAndTradeDateFetchCurrencyPair(any(), any())).thenReturn(positions);

        when(eodCalculator.rebalanceLPPositions(any())).thenReturn(
            Collections.singletonMap(
                EURUSD,
                List.of(
                    new BalanceTrade(PARTICIPANT_A, PARTICIPANT_C, BigDecimal.valueOf(-123539000)),
                    new BalanceTrade(PARTICIPANT_A, PARTICIPANT_D, BigDecimal.valueOf(-25761000)),
                    new BalanceTrade(PARTICIPANT_B, PARTICIPANT_D, BigDecimal.valueOf(-21100000)),
                    new BalanceTrade(PARTICIPANT_A, PARTICIPANT_D, BigDecimal.valueOf(-100000))
                )
            )
        );

        when(eodCalculator.netAllTtrades(any())).thenReturn(
            Stream.of(ParticipantPositionForPair.of(PARTICIPANT_A, EURUSD, BigDecimal.TEN))
        );

        final JobExecution execution = jobLauncherTestUtils.launchStep(REBALANCE_POSITIONS_STEP_NAME,
            new JobParametersBuilder(jobLauncherTestUtils.getUniqueJobParameters())
                .addString(BUSINESS_DATE_JOB_PARAM_NAME, businessDateStr)
                .toJobParameters());

        assertThat(execution.getStatus()).isSameAs(BatchStatus.COMPLETED);

        verify(participantPositionRepository).findAllByPositionTypeAndTradeDateFetchCurrencyPair(ParticipantPositionType.NET, businessDate);

        verify(eodCalculator).rebalanceLPPositions(positions);

        verify(tradeRepository).saveAll(tradeCaptor.capture());
        assertThat(tradeCaptor.getValue())
            .extracting(
                TradeEntity::getOriginator,
                TradeEntity::getCounterparty,
                trade -> trade.getBaseAmount().getValue().longValue(),
                TradeEntity::getDirection,
                TradeEntity::getSpotRate,
                TradeEntity::getCurrencyPair,
                TradeEntity::getTradeDate,
                TradeEntity::getValueDate
            ).containsExactlyInAnyOrder(
                tuple(ORIG_A, ORIG_C, 123539000L, Side.SELL, EURUSD_RATE, EURUSD, businessDate, valueDate),
                tuple(ORIG_A, ORIG_D, 25761000L, Side.SELL, EURUSD_RATE, EURUSD, businessDate, valueDate),
                tuple(ORIG_B, ORIG_D, 21100000L, Side.SELL, EURUSD_RATE, EURUSD, businessDate, valueDate),
                tuple(ORIG_A, ORIG_D, 100000L, Side.SELL, EURUSD_RATE, EURUSD, businessDate, valueDate)
            );

        verify(eodCalculator).netAllTtrades(netCaptor.capture());
        assertThat(netCaptor.getValue())
            .extracting(CcyParticipantAmount::getCurrencyPair, CcyParticipantAmount::getParticipant, CcyParticipantAmount::getAmount)
            .containsExactlyInAnyOrder(
                tuple(EURUSD, PARTICIPANT_A, BigDecimal.valueOf(-123539000)),
                tuple(EURUSD, PARTICIPANT_C, BigDecimal.valueOf(123539000)),
                tuple(EURUSD, PARTICIPANT_A, BigDecimal.valueOf(-25761000L)),
                tuple(EURUSD, PARTICIPANT_D, BigDecimal.valueOf(25761000L)),
                tuple(EURUSD, PARTICIPANT_B, BigDecimal.valueOf(-21100000L)),
                tuple(EURUSD, PARTICIPANT_D, BigDecimal.valueOf(21100000L)),
                tuple(EURUSD, PARTICIPANT_A, BigDecimal.valueOf(-100000L)),
                tuple(EURUSD, PARTICIPANT_D, BigDecimal.valueOf(100000L))
            );

        verify(participantPositionRepository).saveAll(positionCaptor.capture());
        assertThat(positionCaptor.getValue())
            .extracting(
                ParticipantPositionEntity::getCurrencyPair,
                ParticipantPositionEntity::getParticipant,
                position -> position.getAmount().getValue(),
                ParticipantPositionEntity::getPrice,
                ParticipantPositionEntity::getTradeDate,
                ParticipantPositionEntity::getValueDate
            ).containsOnly(
                tuple(EURUSD, PARTICIPANT_A, BigDecimal.TEN, EURUSD_RATE, businessDate, valueDate)
            );

        verifyNoMoreInteractions(tradeRepository, eodCalculator, participantPositionRepository);


    }

    @TestConfiguration
    @ComponentScan(basePackageClasses = {
        RebalancingTasklet.class, TradeOrPositionEssentialsMapper.class, ParticipantPositionRepository.class,
        EODCalculator.class
    },
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
            classes = {
                RebalancingTasklet.class,
                EODCalculator.class,
                TradeRepository.class,
                TradeOrPositionEssentialsMapper.class,
                ParticipantPositionRepository.class
            })
    )
    static class TestConfig {

    }
}