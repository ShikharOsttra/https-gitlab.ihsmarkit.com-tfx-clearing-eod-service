package com.ihsmarkit.tfx.eod.statemachine;

import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.Events.EOD;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.READY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@Import(StateMachineConfig.class)
class StateMachineConfigTest {

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

    @BeforeEach
    void startMachine() {
        stateMachine.start();
    }

    @AfterEach
    void stopMachine() {
        stateMachine.stop();
    }

    private StateMachineTestPlanBuilder<StateMachineConfig.States, StateMachineConfig.Events> testPlanBuilder() {
        return StateMachineTestPlanBuilder.<StateMachineConfig.States, StateMachineConfig.Events>builder();
    }

    @Test
    void shouldRunWhenDspApprovedFirst() throws Exception {

        when(dspApprovedGuard.evaluate(any())).thenReturn(false, false, true);
        when(tradesInFlightGuard.evaluate(any())).thenReturn(false, false, false, true);
        when(swpPointApprovedGuard.evaluate(any())).thenReturn(false, true);

        testPlanBuilder()
            .stateMachine(stateMachine)
            .step()
                .sendEvent(EOD)
                .expectStateChanged(17)
                .expectState(READY)
            .and().build().test();

        verify(dspApprovedGuard, times(3)).evaluate(any());
        verify(tradesInFlightGuard, times(4)).evaluate(any());
        verify(swpPointApprovedGuard, times(2)).evaluate(any());

        verify(eod1runAction).execute(any());
        verify(eod1CompleteAction).execute(any());
        verify(eod2runAction).execute(any());
        verify(eod2CompleteAction).execute(any());
        verify(dateRollRunAction).execute(any());
    }

    @Test
    void shouldReturnToIdleOnError() throws Exception {

        when(dspApprovedGuard.evaluate(any())).thenReturn(true);
        when(tradesInFlightGuard.evaluate(any())).thenReturn(true);
        when(swpPointApprovedGuard.evaluate(any())).thenReturn(true);

        doAnswer(invocation -> {
            ((StateContext) invocation.getArgument(0)).getStateMachine().setStateMachineError(new RuntimeException());
            return null;
        }).when(eod1runAction).execute(any());

        testPlanBuilder()
            .stateMachine(stateMachine)
            .step()
                .sendEvent(EOD)
                .expectStateChanged(6)
                .expectState(READY)
            .and().build().test();

        verify(eod1runAction).execute(any());
        verifyNoMoreInteractions(eod1CompleteAction, eod2CompleteAction, eod2runAction, dateRollRunAction);
        assertThat(stateMachine.getState().getId()).isEqualTo(READY);

    }

    @Test
    void shouldRunWhenNoTradesInFlightBeforeDspApproval() throws Exception {

        when(dspApprovedGuard.evaluate(any())).thenReturn(false, false, true);
        when(tradesInFlightGuard.evaluate(any())).thenReturn(false, true);
        when(swpPointApprovedGuard.evaluate(any())).thenReturn(true);

        testPlanBuilder()
            .stateMachine(stateMachine)
            .step()
                .sendEvent(EOD)
                .expectStateChanged(14)
                .expectState(READY)
            .and().build().test();

        assertThat(stateMachine.getState().getId()).isEqualTo(READY);
        verify(dspApprovedGuard, times(3)).evaluate(any());
        verify(tradesInFlightGuard, times(2)).evaluate(any());
        verify(swpPointApprovedGuard, times(1)).evaluate(any());

        verify(eod1runAction).execute(any());
        verify(eod1CompleteAction).execute(any());
        verify(eod2runAction).execute(any());
        verify(eod2CompleteAction).execute(any());
        verify(dateRollRunAction).execute(any());
    }

}