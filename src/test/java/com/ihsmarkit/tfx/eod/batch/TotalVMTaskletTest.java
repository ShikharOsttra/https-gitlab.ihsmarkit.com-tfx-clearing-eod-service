package com.ihsmarkit.tfx.eod.batch;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aCurrencyPairEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aParticipantEntityBuilder;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.DAILY_MTM;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.INITIAL_MTM;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.SWAP_PNL;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.TOTAL_VM;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_FMT;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_JOB_PARAM_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.JPY;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.TOTAL_VM_STEP_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodProductCashSettlementRepository;
import com.ihsmarkit.tfx.eod.config.AbstractSpringBatchTest;
import com.ihsmarkit.tfx.eod.config.EOD2JobConfig;
import com.ihsmarkit.tfx.eod.model.ParticipantCurrencyPairAmount;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.TradeAndSettlementDateService;

@Import(EOD2JobConfig.class)
class TotalVMTaskletTest extends AbstractSpringBatchTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2019, 10, 6);
    private static final LocalDate VALUE_DATE = BUSINESS_DATE.plusDays(2);

    private static final ParticipantEntity PARTICIPANT_A = aParticipantEntityBuilder().name("A").build();
    private static final ParticipantEntity PARTICIPANT_B = aParticipantEntityBuilder().name("B").build();

    private static final CurrencyPairEntity CURRENCY_PAIR_USD = aCurrencyPairEntityBuilder().build();
    private static final CurrencyPairEntity CURRENCY_PAIR_JPY = aCurrencyPairEntityBuilder().baseCurrency(JPY).build();

    private static final EodProductCashSettlementEntity MARGIN_1 = eodProductCashSettlementEntityBuilder().build();
    private static final EodProductCashSettlementEntity MARGIN_2 = eodProductCashSettlementEntityBuilder().type(DAILY_MTM).build();
    private static final EodProductCashSettlementEntity MARGIN_3 = eodProductCashSettlementEntityBuilder().currencyPair(CURRENCY_PAIR_JPY).build();
    private static final EodProductCashSettlementEntity MARGIN_4 = eodProductCashSettlementEntityBuilder().participant(PARTICIPANT_B).build();

    private static final ParticipantCurrencyPairAmount RES1 = ParticipantCurrencyPairAmount.of(PARTICIPANT_A, CURRENCY_PAIR_USD, BigDecimal.TEN);
    private static final ParticipantCurrencyPairAmount RES2 = ParticipantCurrencyPairAmount.of(PARTICIPANT_B, CURRENCY_PAIR_JPY, BigDecimal.ONE);

    @MockBean
    private TradeAndSettlementDateService tradeAndSettlementDateService;

    @MockBean
    private EODCalculator eodCalculator;

    @MockBean
    private EodProductCashSettlementRepository eodProductCashSettlementRepository;

    @Captor
    private ArgumentCaptor<Iterable<EodProductCashSettlementEntity>> marginCptor;

    @Captor
    private ArgumentCaptor<Stream<ParticipantCurrencyPairAmount>> netCaptor;

    @Test
    void shouldCalculateTotalVM() {

        when(tradeAndSettlementDateService.getValueDate(BUSINESS_DATE, CURRENCY_PAIR_USD)).thenReturn(VALUE_DATE);
        when(tradeAndSettlementDateService.getValueDate(BUSINESS_DATE, CURRENCY_PAIR_JPY)).thenReturn(VALUE_DATE);

        when(eodProductCashSettlementRepository.findByDateAndTypeIn(any(), any())).thenReturn(
            Stream.of(MARGIN_1, MARGIN_2, MARGIN_3, MARGIN_4)
        );
        when(eodCalculator.netAll(any())).thenReturn(
            Stream.of(RES1, RES2)
        );

        final JobExecution execution = jobLauncherTestUtils.launchStep(TOTAL_VM_STEP_NAME,
            new JobParametersBuilder(jobLauncherTestUtils.getUniqueJobParameters())
                .addString(BUSINESS_DATE_JOB_PARAM_NAME, BUSINESS_DATE.format(BUSINESS_DATE_FMT))
                .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        verify(eodProductCashSettlementRepository).findByDateAndTypeIn(BUSINESS_DATE, DAILY_MTM, INITIAL_MTM, SWAP_PNL);

        verify(eodCalculator).netAll(netCaptor.capture());
        assertThat(netCaptor.getValue())
            .extracting(
                ParticipantCurrencyPairAmount::getCurrencyPair,
                ParticipantCurrencyPairAmount::getParticipant,
                ParticipantCurrencyPairAmount::getAmount
            ).containsExactlyInAnyOrder(
                tuple(CURRENCY_PAIR_USD, PARTICIPANT_A, BigDecimal.ONE),
                tuple(CURRENCY_PAIR_USD, PARTICIPANT_A, BigDecimal.ONE),
                tuple(CURRENCY_PAIR_JPY, PARTICIPANT_A, BigDecimal.ONE),
                tuple(CURRENCY_PAIR_USD, PARTICIPANT_B, BigDecimal.ONE)
            );

        verify(eodProductCashSettlementRepository).saveAll(marginCptor.capture());
        assertThat(marginCptor.getValue())
            .extracting(
                EodProductCashSettlementEntity::getParticipant,
                EodProductCashSettlementEntity::getCurrencyPair,
                EodProductCashSettlementEntity::getAmount,
                EodProductCashSettlementEntity::getType
            ).containsExactlyInAnyOrder(
                tuple(PARTICIPANT_A, CURRENCY_PAIR_USD, AmountEntity.of(BigDecimal.TEN, "JPY"), TOTAL_VM),
                tuple(PARTICIPANT_B, CURRENCY_PAIR_JPY, AmountEntity.of(BigDecimal.ONE, "JPY"), TOTAL_VM)
            );

        verifyNoMoreInteractions(eodProductCashSettlementRepository, eodCalculator);
    }

    private static EodProductCashSettlementEntity.EodProductCashSettlementEntityBuilder eodProductCashSettlementEntityBuilder() {
        return EodProductCashSettlementEntity
            .builder()
            .amount(AmountEntity.of(BigDecimal.ONE, "JPY"))
            .type(INITIAL_MTM)
            .participant(PARTICIPANT_A)
            .currencyPair(CURRENCY_PAIR_USD);
    }
}