package com.ihsmarkit.tfx.eod.integration;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.CASH_BALANCE_UPDATE_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD1_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.QuartzConfig.BALANCE_UPDATE_JOB_TRIGGER_NAME;
import static com.ihsmarkit.tfx.eod.config.QuartzConfig.EOD1_JOB_TRIGGER1_NAME;
import static com.ihsmarkit.tfx.eod.config.QuartzConfig.EOD1_JOB_TRIGGER2_NAME;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.EOD1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.CronTrigger;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.guard.Guard;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.ihsmarkit.tfx.alert.client.jms.AlertSender;
import com.ihsmarkit.tfx.core.dl.repository.SystemParameterRepository;
import com.ihsmarkit.tfx.core.dl.repository.calendar.CalendarTradingSwapPointRepository;
import com.ihsmarkit.tfx.eod.batch.CashCollateralBalanceUpdateTasklet;
import com.ihsmarkit.tfx.eod.config.CashCollateralBalanceUpdateJobConfig;
import com.ihsmarkit.tfx.eod.config.QuartzConfig;
import com.ihsmarkit.tfx.eod.config.listeners.EodFailedStepAlertSender;
import com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig;
import com.ihsmarkit.tfx.eod.statemachine.StateWaitingListener;
import com.ihsmarkit.tfx.test.utils.db.DbUnitTestListeners;

@ExtendWith(SpringExtension.class)
@ImportAutoConfiguration({
    QuartzAutoConfiguration.class,
    com.ihsmarkit.tfx.core.config.QuartzConfig.class
})
@ContextConfiguration(classes = {
    IntegrationTestConfig.class,
    QuartzConfig.class,
    StateMachineConfig.class,
    CashCollateralBalanceUpdateJobConfig.class
})
@TestPropertySource(
    locations = "classpath:/application.properties",
    properties = {
        "eod1.job.enabled=true",
        "cashCollateralBalanceUpdate.job.enabled=true",
        "eod1.job.trigger1.cron=0 0 1 1 1 ? 2099",
        "eod1.job.trigger2.cron=0 0 1 2 1 ? 2099",
        "cashCollateralBalanceUpdate.job.trigger.cron=0 0 1 3 1 ? 2099"
    }
)
@DbUnitTestListeners
@DatabaseTearDown("/common/tearDown.xml")
public class QuartzIntegrationTest {

    private static final LocalDate OCT_7 = LocalDate.of(2019, 10, 7);

    @Autowired
    private StateMachine<StateMachineConfig.States, StateMachineConfig.Events> stateMachine;

    @MockBean
    private AlertSender alertSender;

    @MockBean(name = "dspApprovedGuard")
    private Guard<StateMachineConfig.States, StateMachineConfig.Events> dspApprovedGuard;

    @MockBean(name = "eodDateCheckGuard")
    private Guard<StateMachineConfig.States, StateMachineConfig.Events> eodDateCheckGuard;

    @MockBean(name = "tradesInFlightGuard")
    private Guard<StateMachineConfig.States, StateMachineConfig.Events> tradesInFlightGuard;

    @MockBean(name = "swpPointApprovedGuard")
    private Guard<StateMachineConfig.States, StateMachineConfig.Events> swpPointApprovedGuard;

    @MockBean(name = "eod1runAction")
    private Action<StateMachineConfig.States, StateMachineConfig.Events> eod1runAction;

    @MockBean(name = "initAction")
    private Action<StateMachineConfig.States, StateMachineConfig.Events> initAction;

    @MockBean(name = "eodPrematureAction")
    private Action<StateMachineConfig.States, StateMachineConfig.Events> eodPrematureAction;

    @MockBean(name = "eod1CompleteAction")
    private Action<StateMachineConfig.States, StateMachineConfig.Events> eod1CompleteAction;

    @MockBean(name = "eod2CompleteAction")
    private Action<StateMachineConfig.States, StateMachineConfig.Events> eod2CompleteAction;

    @MockBean(name = "eod2runAction")
    private Action<StateMachineConfig.States, StateMachineConfig.Events> eod2runAction;

    @MockBean(name = "dateRollRunAction")
    private Action<StateMachineConfig.States, StateMachineConfig.Events> dateRollRunAction;

    @MockBean
    private SystemParameterRepository systemParameterRepository;

    @MockBean
    private CalendarTradingSwapPointRepository calendarTradingSwapPointRepository;

    @MockBean
    private CashCollateralBalanceUpdateTasklet cashCollateralBalanceUpdateTasklet;

    @MockBean
    private EodFailedStepAlertSender eodFailedStepAlertSender;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    @Qualifier(EOD1_JOB_TRIGGER1_NAME)
    private CronTrigger eod1JobTrigger1;

    @Autowired
    @Qualifier(EOD1_JOB_TRIGGER2_NAME)
    private CronTrigger eod1JobTrigger2;

    @Autowired(required = false)
    @Qualifier(BALANCE_UPDATE_JOB_TRIGGER_NAME)
    private CronTrigger cashCollateralBalanceUpdateJobTrigger;

    @Test
    void shouldTriggerExecution() throws SchedulerException, InterruptedException {
        final StateWaitingListener listener = new StateWaitingListener(EOD1);
        stateMachine.addStateListener(listener);

        when(eodDateCheckGuard.evaluate(any())).thenReturn(true);

        scheduler.triggerJob(new JobKey(EOD1_BATCH_JOB_NAME, null));

        listener.await(stateMachine, 5);
        assertThat(stateMachine.getState().getId()).isEqualTo(EOD1);
    }

    @Test
    void shouldTriggerBalanceUpdateExecution() throws Exception {
        when(systemParameterRepository.getParameterValueFailFast(any())).thenReturn(OCT_7);
        when(calendarTradingSwapPointRepository.findPreviousTradingDate(any())).thenReturn(Optional.of(OCT_7));

        final CountDownLatch finished = new CountDownLatch(1);

        doAnswer(
            invocation -> {
                finished.countDown();
                return null;
            }
        ).when(cashCollateralBalanceUpdateTasklet).execute(any(), any());

        scheduler.triggerJob(new JobKey(CASH_BALANCE_UPDATE_BATCH_JOB_NAME, null));

        assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void cashCollateralBalanceUpdateJobTrigger1ShouldBeConfigured() {
        assertTriggerFireTime(cashCollateralBalanceUpdateJobTrigger, LocalDate.ofYearDay(2099, 3));
    }

    @Test
    void eodTrigger2ShouldBeConfigured() {
        assertTriggerFireTime(eod1JobTrigger2, LocalDate.ofYearDay(2099, 2));
    }

    @Test
    void eodTrigger1ShouldBeConfigured() {
        assertTriggerFireTime(eod1JobTrigger1, LocalDate.ofYearDay(2099, 1));
    }

    void assertTriggerFireTime(CronTrigger cronTrigger, LocalDate expected) {
        assertThat(cronTrigger)
            .isNotNull()
            .extracting(trigger -> LocalDate.ofInstant(trigger.getNextFireTime().toInstant(), trigger.getTimeZone().toZoneId()))
            .isEqualTo(expected);
    }
}
