package com.ihsmarkit.tfx.eod.integration;

import static com.ihsmarkit.tfx.core.domain.eod.EodStage.DSP_APPROVED;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineActionsConfig.BUSINESS_DATE_ATTRIBUTE;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.Events.STOP;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.DSP_NO_TRADES_DELAY;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.EOD1;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.EOD2;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.EOD_PREMATURE;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.INIT;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.NO_DSP_NO_TRADES_DELAY;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.NO_DSP_TRADES_DELAY;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.READY;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.SWP_PNT_DELAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.statemachine.StateMachine;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.ihsmarkit.tfx.alert.client.jms.AlertSender;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodStatusCompositeId;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodStatusEntity;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodStatusRepository;
import com.ihsmarkit.tfx.core.time.ClockService;
import com.ihsmarkit.tfx.eod.config.EOD1JobConfig;
import com.ihsmarkit.tfx.eod.config.EOD2JobConfig;
import com.ihsmarkit.tfx.eod.config.RollBusinessDateJobConfig;
import com.ihsmarkit.tfx.eod.statemachine.StateMachineActionsConfig;
import com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig;
import com.ihsmarkit.tfx.eod.statemachine.StateWaitingListener;
import com.ihsmarkit.tfx.test.utils.db.DbUnitTestListeners;

@ExtendWith(SpringExtension.class)
@DbUnitTestListeners
@DatabaseTearDown("/common/tearDown.xml")
@ImportAutoConfiguration({
    QuartzAutoConfiguration.class,
    com.ihsmarkit.tfx.core.config.QuartzConfig.class
})
@ContextConfiguration(classes = {
    IntegrationTestConfig.class,
    EOD1JobConfig.class,
    EOD2JobConfig.class,
    RollBusinessDateJobConfig.class,
    StateMachineConfig.class,
    StateMachineActionsConfig.class
})
@TestPropertySource("classpath:/application.properties")
class StateMachineIntegrationTest {

    private static final int WAITING_TIME = 30;

    private static final LocalDate JAN_1 = LocalDate.of(2019, 1, 1);
    private static final LocalDate JAN_2 = JAN_1.plusDays(1);
    private static final LocalDate JAN_3 = JAN_1.plusDays(2);
    private static final LocalDate JAN_4 = JAN_1.plusDays(3);
    private static final LocalDate JAN_5 = JAN_1.plusDays(4);
    private static final LocalDate JAN_7 = JAN_1.plusDays(6);
    private static final LocalDate JAN_8 = JAN_1.plusDays(7);
    private static final LocalDate JAN_9 = JAN_1.plusDays(8);
    private static final LocalDate JAN_10 = JAN_1.plusDays(9);
    private static final LocalDate JAN_11 = JAN_1.plusDays(10);

    @Autowired
    private StateMachine<StateMachineConfig.States, StateMachineConfig.Events> stateMachine;

    @Autowired
    private EodStatusRepository eodStatusRepository;

    @MockBean
    private AlertSender alertSender;

    @MockBean
    private ClockService clockService;

    @Test
    @DatabaseSetup({
        "/common/currency.xml",
        "/common/participants.xml",
        "/statemachine/business_date_2019_1_1.xml"
    })
    void shouldWaitDSP() throws Exception {

        when(clockService.getCurrentDateTimeUTC()).thenReturn(LocalDateTime.of(JAN_2, LocalTime.MIDNIGHT));
        when(clockService.getCurrentDateTime()).thenReturn(LocalDateTime.of(JAN_2, LocalTime.MIDNIGHT));

        resetToReady();
        waitFor(NO_DSP_TRADES_DELAY, WAITING_TIME, () -> stateMachine.sendEvent(StateMachineConfig.Events.EOD));

        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE_ATTRIBUTE)).isEqualTo(JAN_1);
        assertThat(stateMachine.getState().getIds()).containsOnly(EOD1, NO_DSP_TRADES_DELAY);

    }

    @Test
    @DatabaseSetup({
        "/common/currency.xml",
        "/common/participants.xml",
        "/statemachine/business_date_2019_1_1.xml"
    })
    void shouldSkipExecution() throws Exception {

        when(clockService.getCurrentDateTimeUTC()).thenReturn(LocalDateTime.of(JAN_1, LocalTime.MIDNIGHT));
        when(clockService.getCurrentDateTime()).thenReturn(LocalDateTime.of(JAN_1, LocalTime.MIDNIGHT));

        resetToReady();
        waitFor(EOD_PREMATURE, WAITING_TIME, () -> stateMachine.sendEvent(StateMachineConfig.Events.EOD));

        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE_ATTRIBUTE)).isEqualTo(JAN_1);
        assertThat(stateMachine.getState().getIds()).containsOnly(READY);

    }

    @Test
    @DatabaseSetup({
        "/common/currency.xml",
        "/common/participants.xml",
        "/statemachine/business_date_2019_1_1.xml"
    })
    void shouldWaitDSPAndProceedToSwpPnt() throws InterruptedException {

        when(clockService.getCurrentDateTimeUTC()).thenReturn(LocalDateTime.of(JAN_2, LocalTime.MIDNIGHT));
        when(clockService.getCurrentDateTime()).thenReturn(LocalDateTime.of(JAN_2, LocalTime.MIDNIGHT));

        resetToReady();
        waitFor(DSP_NO_TRADES_DELAY, WAITING_TIME, () -> stateMachine.sendEvent(StateMachineConfig.Events.EOD));


        waitFor(SWP_PNT_DELAY, WAITING_TIME, () -> eodStatusRepository.save(
            EodStatusEntity.builder()
                .id(new EodStatusCompositeId(DSP_APPROVED, JAN_1))
                .timestamp(LocalDateTime.now())
                .build()
        ));

        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE_ATTRIBUTE)).isEqualTo(JAN_1);
        assertThat(stateMachine.getState().getIds()).containsOnly(EOD2, SWP_PNT_DELAY);

    }

    @Test
    @DatabaseSetup({
        "/common/currency.xml",
        "/common/participants.xml",
        "/statemachine/business_date_2019_1_2.xml"
    })
    void shouldWaitTradesAndDSP() throws Exception {

        when(clockService.getCurrentDateTimeUTC()).thenReturn(LocalDateTime.of(JAN_3, LocalTime.MIDNIGHT));
        when(clockService.getCurrentDateTime()).thenReturn(LocalDateTime.of(JAN_3, LocalTime.MIDNIGHT));

        resetToReady();
        waitFor(NO_DSP_NO_TRADES_DELAY, WAITING_TIME, () -> stateMachine.sendEvent(StateMachineConfig.Events.EOD));

        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE_ATTRIBUTE)).isEqualTo(JAN_2);
        assertThat(stateMachine.getState().getIds()).containsOnly(EOD1, NO_DSP_NO_TRADES_DELAY);

    }

    @Test
    @DatabaseSetup({
        "/common/currency.xml",
        "/common/participants.xml",
        "/statemachine/business_date_2019_1_3.xml"
    })
    void shouldWaitTrades() throws Exception {

        when(clockService.getCurrentDateTimeUTC()).thenReturn(LocalDateTime.of(JAN_4, LocalTime.MIDNIGHT));
        when(clockService.getCurrentDateTime()).thenReturn(LocalDateTime.of(JAN_4, LocalTime.MIDNIGHT));

        resetToReady();
        waitFor(DSP_NO_TRADES_DELAY, WAITING_TIME, () -> stateMachine.sendEvent(StateMachineConfig.Events.EOD));

        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE_ATTRIBUTE)).isEqualTo(JAN_3);
        assertThat(stateMachine.getState().getIds()).containsOnly(EOD1, DSP_NO_TRADES_DELAY);

    }

    @Test
    @DatabaseSetup({
        "/common/currency.xml",
        "/common/participants.xml",
        "/statemachine/business_date_2019_1_4.xml"
    })
    void shouldWaitSwapPoints() throws Exception {

        when(clockService.getCurrentDateTimeUTC()).thenReturn(LocalDateTime.of(JAN_5, LocalTime.MIDNIGHT));
        when(clockService.getCurrentDateTime()).thenReturn(LocalDateTime.of(JAN_5, LocalTime.MIDNIGHT));

        resetToReady();
        waitFor(SWP_PNT_DELAY, WAITING_TIME, () -> stateMachine.sendEvent(StateMachineConfig.Events.EOD));

        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE_ATTRIBUTE)).isEqualTo(JAN_4);
        assertThat(stateMachine.getState().getIds()).containsOnly(EOD2, SWP_PNT_DELAY);

    }

    @Test
    @DatabaseSetup({
        "/common/currency.xml",
        "/common/fx_spot_product.xml",
        "/common/participants.xml",
        "/common/tradingHours.xml",
        "/statemachine/business_date_2019_1_7.xml",
        "/common/evaluation_date_2019_10_07.xml"
    })
    @ExpectedDatabase(value = "/statemachine/business_date_2019_1_7-expected.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void shouldRunFullCycle() throws Exception {

        when(clockService.getCurrentDateTimeUTC()).thenReturn(LocalDateTime.of(JAN_8, LocalTime.MIDNIGHT));
        when(clockService.getCurrentDateTime()).thenReturn(LocalDateTime.of(JAN_8, LocalTime.MIDNIGHT));

        resetToReady();
        waitFor(INIT, WAITING_TIME, () -> stateMachine.sendEvent(StateMachineConfig.Events.EOD));
        waitFor(READY, WAITING_TIME);

        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE_ATTRIBUTE)).isEqualTo(JAN_7);
        assertThat(stateMachine.getState().getIds()).containsOnly(READY);

    }

    @Test
    @DatabaseSetup({
        "/common/currency.xml",
        "/common/fx_spot_product.xml",
        "/common/participants.xml",
        "/common/tradingHours.xml",
        "/statemachine/business_date_2019_1_8.xml",
        "/common/evaluation_date_2019_10_07.xml"
    })
    void shouldNotWaitCollateralPricesWhenNotBankBusinessDate() throws Exception {

        when(clockService.getCurrentDateTimeUTC()).thenReturn(LocalDateTime.of(JAN_9, LocalTime.MIDNIGHT));
        when(clockService.getCurrentDateTime()).thenReturn(LocalDateTime.of(JAN_9, LocalTime.MIDNIGHT));
        when(clockService.getCurrentDate()).thenReturn(JAN_9);

        resetToReady();
        waitFor(INIT, WAITING_TIME, () -> stateMachine.sendEvent(StateMachineConfig.Events.EOD));
        waitFor(READY, WAITING_TIME);

        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE_ATTRIBUTE)).isEqualTo(JAN_8);
        assertThat(stateMachine.getState().getIds()).containsOnly(READY);
    }

    @Test
    @DatabaseSetup({
        "/common/currency.xml",
        "/statemachine/business_date_2019_1_9.xml"
    })
    void shouldWaitBondPrices() throws Exception {

        when(clockService.getCurrentDateTimeUTC()).thenReturn(LocalDateTime.of(JAN_10, LocalTime.MIDNIGHT));
        when(clockService.getCurrentDateTime()).thenReturn(LocalDateTime.of(JAN_10, LocalTime.MIDNIGHT));
        when(clockService.getCurrentDate()).thenReturn(JAN_10);

        resetToReady();
        waitFor(SWP_PNT_DELAY, WAITING_TIME, () -> stateMachine.sendEvent(StateMachineConfig.Events.EOD));

        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE_ATTRIBUTE)).isEqualTo(JAN_9);
        assertThat(stateMachine.getState().getIds()).containsOnly(EOD2, SWP_PNT_DELAY);
    }

    @Test
    @DatabaseSetup({
        "/common/currency.xml",
        "/statemachine/business_date_2019_1_10.xml"
    })
    void shouldWaitEquityPrices() throws Exception {

        when(clockService.getCurrentDateTimeUTC()).thenReturn(LocalDateTime.of(JAN_11, LocalTime.MIDNIGHT));
        when(clockService.getCurrentDateTime()).thenReturn(LocalDateTime.of(JAN_11, LocalTime.MIDNIGHT));
        when(clockService.getCurrentDate()).thenReturn(JAN_11);

        resetToReady();
        waitFor(SWP_PNT_DELAY, WAITING_TIME, () -> stateMachine.sendEvent(StateMachineConfig.Events.EOD));

        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE_ATTRIBUTE)).isEqualTo(JAN_10);
        assertThat(stateMachine.getState().getIds()).containsOnly(EOD2, SWP_PNT_DELAY);
    }

    void waitFor(final StateMachineConfig.States state, final int seconds) throws InterruptedException {
        waitFor(state, seconds, null);
    }

    void waitFor(final StateMachineConfig.States state, final int seconds, final Runnable toRun) throws InterruptedException {
        final StateWaitingListener listener = new StateWaitingListener(state);
        stateMachine.addStateListener(listener);
        if (toRun != null) {
            toRun.run();
        }
        listener.await(stateMachine, seconds);
    }

    void resetToReady() throws InterruptedException {
        if (stateMachine.getState().getId() != READY) {
            waitFor(READY, WAITING_TIME, () -> stateMachine.sendEvent(STOP));
        }
    }

}
