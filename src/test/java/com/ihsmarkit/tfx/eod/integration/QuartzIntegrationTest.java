package com.ihsmarkit.tfx.eod.integration;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.CASH_BALANCE_UPDATE_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD1_BATCH_JOB_NAME;
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
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.guard.Guard;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ihsmarkit.tfx.core.dl.repository.SystemParameterRepository;
import com.ihsmarkit.tfx.core.dl.repository.calendar.CalendarTradingSwapPointRepository;
import com.ihsmarkit.tfx.eod.batch.CashCollateralBalanceUpdateTasklet;
import com.ihsmarkit.tfx.eod.config.CashCollateralBalanceUpdateJobConfig;
import com.ihsmarkit.tfx.eod.config.QuartzConfig;
import com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig;
import com.ihsmarkit.tfx.eod.statemachine.StateWaitingListener;

@ExtendWith(SpringExtension.class)
@Import({IntegrationTestConfig.class, QuartzConfig.class, StateMachineConfig.class, CashCollateralBalanceUpdateJobConfig.class})
@TestPropertySource(
    locations = "classpath:/application.properties",
    properties = {"eod1.job.enabled=true", "cashCollateralBalanceUpdate.job.enabled=true", "logging.level.org.quartz=DEBUG"}
)
public class QuartzIntegrationTest {

    private static final LocalDate OCT_7 = LocalDate.of(2019, 10, 7);

    @Autowired
    private StateMachine<StateMachineConfig.States, StateMachineConfig.Events> stateMachine;

    @MockBean(name = "dspApprovedGuard")
    private Guard<StateMachineConfig.States, StateMachineConfig.Events> dspApprovedGuard;

    @MockBean(name = "tradesInFlightGuard")
    private Guard<StateMachineConfig.States, StateMachineConfig.Events> tradesInFlightGuard;

    @MockBean(name = "swpPointApprovedGuard")
    private Guard<StateMachineConfig.States, StateMachineConfig.Events> swpPointApprovedGuard;

    @MockBean(name = "eod1runAction")
    private Action<StateMachineConfig.States, StateMachineConfig.Events> eod1runAction;

    @MockBean(name = "initAction")
    private Action<StateMachineConfig.States, StateMachineConfig.Events> initAction;

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

    @Autowired
    private Scheduler scheduler;

    @Test
    void shouldTriggerExection() throws SchedulerException, InterruptedException {
        final StateWaitingListener listener = new StateWaitingListener(EOD1);
        stateMachine.addStateListener(listener);
        scheduler.triggerJob(new JobKey(EOD1_BATCH_JOB_NAME, null));
        listener.await(stateMachine, 5);
        assertThat(stateMachine.getState().getId()).isEqualTo(EOD1);
    }

    @Test
    void shouldTriggerBalanceUpdateExection() throws Exception {
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

}
