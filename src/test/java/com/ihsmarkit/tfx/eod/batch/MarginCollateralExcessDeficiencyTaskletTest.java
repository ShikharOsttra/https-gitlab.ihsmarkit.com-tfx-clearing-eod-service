package com.ihsmarkit.tfx.eod.batch;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aCurrencyPairEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aParticipantEntityBuilder;
import static com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType.DAY;
import static com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType.FOLLOWING;
import static com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType.TOTAL;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.DAILY_MTM;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.INITIAL_MTM;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.TOTAL_VM;
import static com.ihsmarkit.tfx.core.domain.type.MarginAlertAction.BOTH;
import static com.ihsmarkit.tfx.core.domain.type.MarginAlertLevel.ALERT;
import static com.ihsmarkit.tfx.core.domain.type.MarginAlertLevel.HALT;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.NET;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.REBALANCING;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_FMT;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_JOB_PARAM_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.JPY;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.MARGIN_COLLATERAL_EXCESS_OR_DEFICIENCY;
import static java.math.BigDecimal.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import com.ihsmarkit.tfx.core.dl.CollateralTestDataFactory;
import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.MarginAlertConfigurationEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.CashCollateralProductEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralBalanceEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralProductEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.LogCollateralProductEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.haircut.EquityHaircutRateEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.haircut.LogHaircutRateEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodParticipantMarginEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.repository.MarginAlertConfigurationRepository;
import com.ihsmarkit.tfx.core.dl.repository.SystemParameterRepository;
import com.ihsmarkit.tfx.core.dl.repository.collateral.CollateralBalanceRepository;
import com.ihsmarkit.tfx.core.dl.repository.collateral.HaircutRateRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodCashSettlementRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodParticipantMarginRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodProductCashSettlementRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.CollateralPurpose;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.core.domain.type.SystemParameters;
import com.ihsmarkit.tfx.eod.config.AbstractSpringBatchTest;
import com.ihsmarkit.tfx.eod.config.EOD2JobConfig;
import com.ihsmarkit.tfx.eod.service.CalendarDatesProvider;
import com.ihsmarkit.tfx.eod.service.JPYRateService;
import com.ihsmarkit.tfx.eod.service.MarginRatioService;

@ContextConfiguration(classes = EOD2JobConfig.class)
class MarginCollateralExcessDeficiencyTaskletTest extends AbstractSpringBatchTest {

    private static final CurrencyPairEntity CURRENCY_PAIR_EURUSD = aCurrencyPairEntityBuilder().baseCurrency("EUR").valueCurrency("USD").build();
    private static final CurrencyPairEntity CURRENCY_PAIR_USDJPY = aCurrencyPairEntityBuilder().valueCurrency(JPY).build();
    private static final CurrencyPairEntity CURRENCY_PAIR_NZDJPY = aCurrencyPairEntityBuilder().baseCurrency("NZD").valueCurrency(JPY).build();

    private static final CashCollateralProductEntity CASH_PRODUCT = CollateralTestDataFactory.aCashCollateralProductEntityBuilder().build();
    private static final LogCollateralProductEntity LOG_PRODUCT = CollateralTestDataFactory.aLogCollateralProductEntityBuilder().build();

    private static final ParticipantEntity PARTICIPANT1 = aParticipantEntityBuilder().id(1L).build();
    private static final ParticipantEntity PARTICIPANT2 = aParticipantEntityBuilder().id(2L).build();

    private static final LocalDate OCT_6 = LocalDate.of(2019, 10, 6);
    private static final LocalDate OCT_7 = OCT_6.plusDays(1);
    private static final LocalDate OCT_8 = OCT_6.plusDays(2);
    private static final LocalDate OCT_9 = OCT_6.plusDays(3);
    private static final LocalDate OCT_5 = OCT_6.minusDays(1);


    @MockBean
    private CalendarDatesProvider calendarDatesProvider;

    @MockBean
    private EodProductCashSettlementRepository eodProductCashSettlementRepository;

    @MockBean
    private MarginAlertConfigurationRepository marginAlertConfigurationRepository;

    @MockBean
    private EodCashSettlementRepository eodCashSettlementRepository;

    @MockBean
    private ParticipantPositionRepository participantPositionRepository;

    @MockBean
    private MarginRatioService marginRatioService;

    @MockBean
    private JPYRateService jpyRateService;

    @MockBean
    private EodParticipantMarginRepository eodParticipantMarginRepository;

    @MockBean
    private CollateralBalanceRepository collateralBalanceRepository;

    @MockBean
    private HaircutRateRepository haircutRateRepository;

    @MockBean
    private SystemParameterRepository systemParameterRepository;

    @Captor
    private ArgumentCaptor<Iterable<EodCashSettlementEntity>> settlementCaptor;

    @Captor
    private ArgumentCaptor<Iterable<EodParticipantMarginEntity>> marginCaptor;

    private EodProductCashSettlementEntity settlement(
        CurrencyPairEntity currencyPair,
        ParticipantEntity participant,
        LocalDate date,
        LocalDate settlementDate,
        Double amnt,
        EodProductCashSettlementType type
    ) {
        return EodProductCashSettlementEntity.builder()
            .currencyPair(currencyPair)
            .participant(participant)
            .settlementDate(settlementDate)
            .date(date)
            .amount(AmountEntity.of(BigDecimal.valueOf(amnt), "JPY"))
            .type(type)
            .build();
    }

    private ParticipantPositionEntity position(
        ParticipantEntity participant,
        CurrencyPairEntity currencyPair,
        ParticipantPositionType type,
        double price,
        double amnt
    ) {
        return ParticipantPositionEntity.builder()
            .participant(participant)
            .currencyPair(currencyPair)
            .price(BigDecimal.valueOf(price))
            .amount(AmountEntity.of(valueOf(amnt), "IGNORE"))
            .type(type)
            .build();
    }


    private CollateralBalanceEntity collateralBalance(ParticipantEntity participant, CollateralProductEntity product, double amount) {
        return CollateralBalanceEntity.builder()
            .participant(participant)
            .product(product)
            .purpose(CollateralPurpose.MARGIN)
            .amount(valueOf(amount))
            .build();
    }

    @Test
    void shouldCalculateAndStoreMarginAndDeficits() {

        when(systemParameterRepository.getParameterValueFailFast(SystemParameters.BUSINESS_DATE)).thenReturn(OCT_6);

        when(calendarDatesProvider.getNextTradingDate(OCT_6)).thenReturn(Optional.of(OCT_7));
        when(calendarDatesProvider.getNextTradingDate(OCT_7)).thenReturn(Optional.of(OCT_8));

        when(haircutRateRepository.findByBusinessDate(OCT_6)).thenReturn(
            List.of(
                EquityHaircutRateEntity.builder().value(BigDecimal.ONE).build(),
                LogHaircutRateEntity.builder().value(valueOf(90)).issuer(LOG_PRODUCT.getIssuer()).build()
            )
        );


        when(marginAlertConfigurationRepository.findAll()).thenReturn(
            List.of(
                MarginAlertConfigurationEntity.builder().participant(PARTICIPANT1).level(ALERT).triggerLevel(valueOf(50)).action(BOTH).build(),
                MarginAlertConfigurationEntity.builder().participant(PARTICIPANT2).level(HALT).triggerLevel(valueOf(50)).action(BOTH).build()
            )
        );

        when(eodProductCashSettlementRepository.findAllBySettlementDateIsGreaterThan(OCT_6))
            .thenReturn(
                Stream.of(
                    settlement(CURRENCY_PAIR_EURUSD, PARTICIPANT1, OCT_5, OCT_7, 1000.0, INITIAL_MTM),
                    settlement(CURRENCY_PAIR_EURUSD, PARTICIPANT1, OCT_5, OCT_7, 1000.0, TOTAL_VM),
                    settlement(CURRENCY_PAIR_EURUSD, PARTICIPANT1, OCT_6, OCT_8, 100.0, DAILY_MTM),
                    settlement(CURRENCY_PAIR_EURUSD, PARTICIPANT1, OCT_6, OCT_8, 100.0, INITIAL_MTM),
                    settlement(CURRENCY_PAIR_EURUSD, PARTICIPANT1, OCT_6, OCT_8, 200.0, TOTAL_VM),
                    settlement(CURRENCY_PAIR_USDJPY, PARTICIPANT1, OCT_6, OCT_8, 300.0, DAILY_MTM),
                    settlement(CURRENCY_PAIR_USDJPY, PARTICIPANT1, OCT_6, OCT_8, 300.0, TOTAL_VM),
                    settlement(CURRENCY_PAIR_USDJPY, PARTICIPANT2, OCT_6, OCT_8, 700.0, DAILY_MTM),
                    settlement(CURRENCY_PAIR_USDJPY, PARTICIPANT2, OCT_6, OCT_8, 700.0, TOTAL_VM),
                    settlement(CURRENCY_PAIR_USDJPY, PARTICIPANT2, OCT_5, OCT_8, 2000.0, INITIAL_MTM),
                    settlement(CURRENCY_PAIR_USDJPY, PARTICIPANT2, OCT_5, OCT_8, 2000.0, TOTAL_VM),
                    settlement(CURRENCY_PAIR_NZDJPY, PARTICIPANT2, OCT_5, OCT_9, 10000.0, INITIAL_MTM),
                    settlement(CURRENCY_PAIR_NZDJPY, PARTICIPANT2, OCT_5, OCT_9, 10000.0, TOTAL_VM)
                )
            );

        when(collateralBalanceRepository.findByParticipantIdAndPurpose(any(), any())).thenReturn(
            List.of(
                collateralBalance(PARTICIPANT1, LOG_PRODUCT, 1000000),
                collateralBalance(PARTICIPANT1, CASH_PRODUCT, 1000000),
                collateralBalance(PARTICIPANT2, CASH_PRODUCT, 3000000)
            )
        );

        when(marginRatioService.getRequiredMarginRatio(CURRENCY_PAIR_EURUSD, PARTICIPANT1)).thenReturn(valueOf(10));
        when(marginRatioService.getRequiredMarginRatio(CURRENCY_PAIR_USDJPY, PARTICIPANT1)).thenReturn(valueOf(20));
        when(marginRatioService.getRequiredMarginRatio(CURRENCY_PAIR_USDJPY, PARTICIPANT2)).thenReturn(valueOf(30));
        when(marginRatioService.getRequiredMarginRatio(CURRENCY_PAIR_NZDJPY, PARTICIPANT2)).thenReturn(valueOf(50));

        when(jpyRateService.getJpyRate(OCT_6, "USD")).thenReturn(valueOf(100));
        when(jpyRateService.getJpyRate(OCT_6, "NZD")).thenReturn(valueOf(66));
        when(jpyRateService.getJpyRate(OCT_6, "EUR")).thenReturn(valueOf(110));

        when(participantPositionRepository.findAllNetAndRebalancingPositionsByTradeDate(OCT_6))
            .thenReturn(
                Stream.of(
                    position(PARTICIPANT1, CURRENCY_PAIR_EURUSD, NET, 1.1, 100000.0),
                    position(PARTICIPANT1, CURRENCY_PAIR_EURUSD, REBALANCING, 1.1, 100000.0),
                    position(PARTICIPANT1, CURRENCY_PAIR_USDJPY, NET, 100.0, 100000.0),
                    position(PARTICIPANT2, CURRENCY_PAIR_USDJPY, NET, 100.0, 100000.0),
                    position(PARTICIPANT2, CURRENCY_PAIR_NZDJPY, NET, 66.0, 100000.0)
                )
            );

        final JobExecution execution = jobLauncherTestUtils.launchStep(
            MARGIN_COLLATERAL_EXCESS_OR_DEFICIENCY,
            new JobParametersBuilder(jobLauncherTestUtils.getUniqueJobParameters())
                .addString(BUSINESS_DATE_JOB_PARAM_NAME, OCT_6.format(BUSINESS_DATE_FMT))
                .toJobParameters()
        );

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        verify(eodCashSettlementRepository).saveAll(settlementCaptor.capture());
        assertThat(settlementCaptor.getValue())
            .extracting(
                EodCashSettlementEntity::getParticipant,
                EodCashSettlementEntity::getDate,
                EodCashSettlementEntity::getType,
                EodCashSettlementEntity::getDateType,
                settlement -> settlement.getAmount().getValue().doubleValue()
            ).containsExactlyInAnyOrder(
                tuple(PARTICIPANT1, OCT_6, DAILY_MTM, FOLLOWING, 400.0),
                tuple(PARTICIPANT1, OCT_6, DAILY_MTM, TOTAL, 400.0),
                tuple(PARTICIPANT1, OCT_6, INITIAL_MTM, FOLLOWING, 100.0),
                tuple(PARTICIPANT1, OCT_6, INITIAL_MTM, DAY, 1000.0),
                tuple(PARTICIPANT1, OCT_6, INITIAL_MTM, TOTAL, 1100.0),
                tuple(PARTICIPANT1, OCT_6, TOTAL_VM, FOLLOWING, 500.0),
                tuple(PARTICIPANT1, OCT_6, TOTAL_VM, DAY, 1000.0),
                tuple(PARTICIPANT1, OCT_6, TOTAL_VM, TOTAL, 1500.0),
                tuple(PARTICIPANT2, OCT_6, DAILY_MTM, FOLLOWING, 700.0),
                tuple(PARTICIPANT2, OCT_6, INITIAL_MTM, FOLLOWING, 2000.0),
                tuple(PARTICIPANT2, OCT_6, DAILY_MTM, TOTAL, 700.0),
                tuple(PARTICIPANT2, OCT_6, INITIAL_MTM, TOTAL, 12000.0),
                tuple(PARTICIPANT2, OCT_6, TOTAL_VM, FOLLOWING, 2700.0),
                tuple(PARTICIPANT2, OCT_6, TOTAL_VM, TOTAL, 12700.0)
        );

        verify(eodParticipantMarginRepository).saveAll(marginCaptor.capture());
        assertThat(marginCaptor.getValue())
            .extracting(
                EodParticipantMarginEntity::getParticipant,
                EodParticipantMarginEntity::getDate,
                EodParticipantMarginEntity::getMarginAlertLevel,
                margin -> margin.getCashCollateral().doubleValue(),
                margin -> margin.getLogCollateral().doubleValue(),
                margin -> margin.getTodaySettlement().doubleValue(),
                margin -> margin.getNextDaySettlement().doubleValue(),
                margin -> margin.getPnl().doubleValue(),
                margin -> margin.getRequiredAmount().doubleValue(),
                margin -> margin.getInitialMargin().doubleValue(),
                margin -> margin.getTotalDeficit().doubleValue(),
                margin -> margin.getCashDeficit().doubleValue(),
                margin -> margin.getMarginRatio().doubleValue()
            ).containsExactlyInAnyOrder(
                tuple(
                    PARTICIPANT1,
                    OCT_6,
                    ALERT,
                    1000000.0,
                    900000.0,
                    1000.0,
                    500.0,
                    1500.0,
                    4198500.0,
                    4200000.0,
                    -2298500.0,
                    1001000.0,
                    45.0
                ),
                tuple(
                    PARTICIPANT2,
                    OCT_6,
                    HALT,
                    3000000.0,
                    0.0,
                    0.0,
                    2700.0,
                    12700.0,
                    6287300.0,
                    6300000.0,
                    -3287300.0,
                    3000000.0,
                    48.0
                )
            );
    }

}