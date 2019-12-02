package com.ihsmarkit.tfx.eod.batch;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aCurrencyPairEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aParticipantEntityBuilder;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_FMT;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_JOB_PARAM_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.JPY;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.NET_TRADES_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.USD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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

import com.ihsmarkit.tfx.core.dl.EntityTestDataFactory;
import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.LegalEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.core.domain.type.ParticipantType;
import com.ihsmarkit.tfx.core.domain.type.Side;
import com.ihsmarkit.tfx.eod.config.DateConfig;
import com.ihsmarkit.tfx.eod.config.EOD1JobConfig;
import com.ihsmarkit.tfx.eod.mapper.TradeOrPositionEssentialsMapper;
import com.ihsmarkit.tfx.eod.model.ParticipantCurrencyPairAmount;
import com.ihsmarkit.tfx.eod.model.TradeOrPositionEssentials;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceProvider;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.TradeAndSettlementDateService;

class NettingTaskletTest extends AbstractSpringBatchTest {

    private static final String BUSINESS_DATE_STR = "20191006";
    private static final LocalDate BUSINESS_DATE = LocalDate.parse(BUSINESS_DATE_STR, BUSINESS_DATE_FMT);
    private static final LocalDate VALUE_DATE = BUSINESS_DATE.plusDays(2);

    private static final CurrencyPairEntity CURRENCY_PAIR_USD = aCurrencyPairEntityBuilder().valueCurrency(JPY).build();
    private static final CurrencyPairEntity CURRENCY_PAIR_JPY = aCurrencyPairEntityBuilder().baseCurrency(JPY).build();
    private static final ParticipantEntity PARTICIPANT = aParticipantEntityBuilder().build();
    private static final LegalEntity ORIGINATOR_A = EntityTestDataFactory.aLegalEntityBuilder()
        .participant(PARTICIPANT)
        .build();

    private static final TradeEntity A_BUYS_20_USD = TradeEntity.builder()
        .direction(Side.BUY)
        .currencyPair(CURRENCY_PAIR_USD)
        .originator(ORIGINATOR_A)
        .spotRate(BigDecimal.valueOf(99.3))
        .baseAmount(AmountEntity.of(BigDecimal.valueOf(20.0), USD))
        .build();

    private static final TradeEntity A_SELLS_10_USD = TradeEntity.builder()
        .direction(Side.SELL)
        .currencyPair(CURRENCY_PAIR_USD)
        .originator(ORIGINATOR_A)
        .spotRate(BigDecimal.valueOf(99.5))
        .baseAmount(AmountEntity.of(BigDecimal.valueOf(10.0), USD))
        .build();

    private static final ParticipantPositionEntity POSITION = ParticipantPositionEntity.builder()
        .currencyPair(CURRENCY_PAIR_USD)
        .participant(PARTICIPANT)
        .amount(AmountEntity.of(BigDecimal.valueOf(993.0), USD))
        .price(BigDecimal.valueOf(99.4))
        .build();

    @MockBean
    private TradeRepository tradeRepository;

    @MockBean
    private ParticipantPositionRepository participantPositionRepository;

    @MockBean
    private DailySettlementPriceProvider dailySettlementPriceProvider;

    @MockBean
    private EODCalculator eodCalculator;

    @MockBean
    private TradeAndSettlementDateService tradeAndSettlementDateService;

    @Mock
    private Stream<TradeEntity> trades;

    @Mock
    private Map<CurrencyPairEntity, BigDecimal> dailySettlementPrices;

    @Captor
    private ArgumentCaptor<Iterable<ParticipantPositionEntity>> positionCaptor;

    @Captor
    private ArgumentCaptor<Stream<TradeOrPositionEssentials>> tradeCaptor;

    @Test
    void shouldCalculateAndStoreNetPosition() {

        when(tradeAndSettlementDateService.getValueDate(BUSINESS_DATE, CURRENCY_PAIR_USD)).thenReturn(VALUE_DATE);
        when(tradeAndSettlementDateService.getValueDate(BUSINESS_DATE, CURRENCY_PAIR_JPY)).thenReturn(VALUE_DATE);

        when(tradeRepository.findAllNovatedForTradeDate(any())).thenReturn(
            Stream.of(A_BUYS_20_USD, A_SELLS_10_USD)
        );

        when(participantPositionRepository.findAllByPositionTypeAndTradeDateFetchCurrencyPair(any(), any())).thenReturn(
            List.of(POSITION)
        );

        when(dailySettlementPriceProvider.getDailySettlementPrices(BUSINESS_DATE))
            .thenReturn(dailySettlementPrices);

        when(dailySettlementPrices.get(CURRENCY_PAIR_USD))
            .thenReturn(BigDecimal.valueOf(2));
        when(dailySettlementPrices.get(CURRENCY_PAIR_JPY))
            .thenReturn(BigDecimal.valueOf(3));

        when(eodCalculator.netAllTtrades(any()))
            .thenReturn(
                Stream.of(
                    ParticipantCurrencyPairAmount.of(PARTICIPANT, CURRENCY_PAIR_USD, BigDecimal.ONE),
                    ParticipantCurrencyPairAmount.of(PARTICIPANT, CURRENCY_PAIR_JPY, BigDecimal.valueOf(2))
                )
            );

        final JobExecution execution = jobLauncherTestUtils.launchStep(NET_TRADES_STEP_NAME,
            new JobParametersBuilder(jobLauncherTestUtils.getUniqueJobParameters())
                .addString(BUSINESS_DATE_JOB_PARAM_NAME, BUSINESS_DATE_STR)
                .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        verify(participantPositionRepository)
            .findAllByPositionTypeAndTradeDateFetchCurrencyPair(ParticipantPositionType.SOD, BUSINESS_DATE);

        verify(eodCalculator).netAllTtrades(tradeCaptor.capture());
        assertThat(tradeCaptor.getValue())
            .extracting(
                TradeOrPositionEssentials::getParticipant,
                TradeOrPositionEssentials::getCurrencyPair,
                TradeOrPositionEssentials::getSpotRate,
                TradeOrPositionEssentials::getAmount
            ).containsExactlyInAnyOrder(
                tuple(PARTICIPANT, CURRENCY_PAIR_USD, BigDecimal.valueOf(99.3), BigDecimal.valueOf(20.0)),
                tuple(PARTICIPANT, CURRENCY_PAIR_USD, BigDecimal.valueOf(99.4), BigDecimal.valueOf(993.0)),
                tuple(PARTICIPANT, CURRENCY_PAIR_USD, BigDecimal.valueOf(99.5), BigDecimal.valueOf(-10.0))
            );

        verify(tradeRepository).findAllNovatedForTradeDate(BUSINESS_DATE);

        verify(participantPositionRepository).saveAll(positionCaptor.capture());
        assertThat(positionCaptor.getValue())
            .extracting(
                ParticipantPositionEntity::getParticipant,
                ParticipantPositionEntity::getParticipantType,
                ParticipantPositionEntity::getCurrencyPair,
                ParticipantPositionEntity::getType,
                ParticipantPositionEntity::getAmount,
                ParticipantPositionEntity::getPrice,
                ParticipantPositionEntity::getTradeDate,
                ParticipantPositionEntity::getValueDate
            )
            .containsExactlyInAnyOrder(
                tuple(
                    PARTICIPANT,
                    ParticipantType.LIQUIDITY_PROVIDER,
                    CURRENCY_PAIR_USD,
                    ParticipantPositionType.NET,
                    AmountEntity.of(BigDecimal.ONE, USD),
                    BigDecimal.valueOf(2),
                    BUSINESS_DATE,
                    VALUE_DATE
                ),
                tuple(
                    PARTICIPANT,
                    ParticipantType.LIQUIDITY_PROVIDER,
                    CURRENCY_PAIR_JPY,
                    ParticipantPositionType.NET,
                    AmountEntity.of(BigDecimal.valueOf(2), JPY),
                    BigDecimal.valueOf(3),
                    BUSINESS_DATE,
                    VALUE_DATE
                )
            );

        verifyNoMoreInteractions(tradeRepository, eodCalculator, participantPositionRepository);

    }

    @TestConfiguration
    @ComponentScan(basePackageClasses = {
        NettingTasklet.class, TradeOrPositionEssentialsMapper.class, ParticipantPositionRepository.class,
        EODCalculator.class, EOD1JobConfig.class, DateConfig.class
    },
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
            classes = {
                NettingTasklet.class,
                EODCalculator.class,
                TradeRepository.class,
                TradeOrPositionEssentialsMapper.class,
                ParticipantPositionRepository.class,
                EOD1JobConfig.class,
                DateConfig.class
            })
    )
    static class TestConfig {

    }

}
