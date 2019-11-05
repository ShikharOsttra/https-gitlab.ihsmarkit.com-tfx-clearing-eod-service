package com.ihsmarkit.tfx.eod.batch;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aCurrencyPairEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aParticipantEntityBuilder;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_FMT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.core.domain.type.ParticipantType;
import com.ihsmarkit.tfx.eod.mapper.TradeOrPositionEssentialsMapper;
import com.ihsmarkit.tfx.eod.model.ParticipantPositionForPair;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceProvider;
import com.ihsmarkit.tfx.eod.service.NetCalculator;

class NettingTaskletTest extends AbstractSpringBatchTest {

    private static final CurrencyPairEntity CURRENCY_PAIR_USD = aCurrencyPairEntityBuilder().build();
    private static final CurrencyPairEntity CURRENCY_PAIR_JPY = aCurrencyPairEntityBuilder().baseCurrency("JPY").build();
    private static final ParticipantEntity PARTICIPANT = aParticipantEntityBuilder().build();

    @MockBean
    private TradeRepository tradeRepository;

    @MockBean
    private ParticipantPositionRepository participantPositionRepository;

    @MockBean
    private DailySettlementPriceProvider dailySettlementPriceProvider;

    @MockBean
    private NetCalculator netCalculator;

    @Mock
    private Stream<TradeEntity> trades;

    @Mock
    private Map<CurrencyPairEntity, BigDecimal> dailySettlementPrices;

    @Captor
    private ArgumentCaptor<Iterable<ParticipantPositionEntity>> captor;

    @Test
    void shouldCalculateAndStoreNetPosition() {
        final String businessDateStr = "20191006";
        final LocalDate businessDate = LocalDate.parse(businessDateStr, BUSINESS_DATE_FMT);

        when(tradeRepository.findAllNovatedForTradeDate(any())).thenReturn(trades);

        when(dailySettlementPriceProvider.getDailySettlementPrices(businessDate))
            .thenReturn(dailySettlementPrices);

        when(dailySettlementPrices.get(CURRENCY_PAIR_USD))
            .thenReturn(BigDecimal.valueOf(2));
        when(dailySettlementPrices.get(CURRENCY_PAIR_JPY))
            .thenReturn(BigDecimal.valueOf(3));

        when(netCalculator.netAllTtrades(any()))
            .thenReturn(
                Stream.of(
                    ParticipantPositionForPair.of(PARTICIPANT, CURRENCY_PAIR_USD, BigDecimal.ONE),
                    ParticipantPositionForPair.of(PARTICIPANT, CURRENCY_PAIR_JPY, BigDecimal.valueOf(2))
                )
            );

        final JobExecution execution = jobLauncherTestUtils.launchStep("netTrades",
            new JobParametersBuilder(jobLauncherTestUtils.getUniqueJobParameters())
                .addString("businessDate", businessDateStr)
                .toJobParameters());

        assertThat(execution.getStatus()).isSameAs(BatchStatus.COMPLETED);

        verify(netCalculator).netAllTtrades(trades);

        verify(tradeRepository).findAllNovatedForTradeDate(businessDate);

        verify(participantPositionRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
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
                Tuple.tuple(
                    PARTICIPANT,
                    ParticipantType.LIQUIDITY_PROVIDER,
                    CURRENCY_PAIR_USD,
                    ParticipantPositionType.NET,
                    AmountEntity.of(BigDecimal.ONE, "USD"),
                    BigDecimal.valueOf(2),
                    businessDate,
                    LocalDate.of(2019, 10, 9)
                ),
                Tuple.tuple(
                    PARTICIPANT,
                    ParticipantType.LIQUIDITY_PROVIDER,
                    CURRENCY_PAIR_JPY,
                    ParticipantPositionType.NET,
                    AmountEntity.of(BigDecimal.valueOf(2), "JPY"),
                    BigDecimal.valueOf(3),
                    businessDate,
                    LocalDate.of(2019, 10, 9)
                )
            );

        verifyNoMoreInteractions(tradeRepository, netCalculator, participantPositionRepository);

    }

    @TestConfiguration
    @ComponentScan(basePackageClasses = {
        NettingTasklet.class, TradeOrPositionEssentialsMapper.class, ParticipantPositionRepository.class,
        NetCalculator.class
    },
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
            classes = {
                NettingTasklet.class,
                NetCalculator.class,
                TradeRepository.class,
                TradeOrPositionEssentialsMapper.class,
                ParticipantPositionRepository.class
            })
    )
    static class TestConfig {

    }

}
