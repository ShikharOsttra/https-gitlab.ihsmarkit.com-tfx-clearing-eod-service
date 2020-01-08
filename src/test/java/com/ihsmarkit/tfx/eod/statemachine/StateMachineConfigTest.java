package com.ihsmarkit.tfx.eod.statemachine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.guard.Guard;
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

    @MockBean(name = "eod2runAction")
    private Action<StateMachineConfig.States, StateMachineConfig.Events> eod2runAction;

    @MockBean(name = "ledgerRunAction")
    private Action<StateMachineConfig.States, StateMachineConfig.Events> ledgerRunAction;

    @MockBean(name = "dateRollRunAction")
    private Action<StateMachineConfig.States, StateMachineConfig.Events> dateRollRunAction;

    @Before
    void startMachine() {
        stateMachine.start();
    }

    @After
    void stopMachine() {
        stateMachine.stop();
    }

    @Test
    void shouldRunWhenDspApprovedFirst() throws Exception {
        stateMachine.start();

        when(dspApprovedGuard.evaluate(any())).thenReturn(false, false, true);
        when(tradesInFlightGuard.evaluate(any())).thenReturn(false, false, false, true);
        when(swpPointApprovedGuard.evaluate(any())).thenReturn(false, true);
        stateMachine.sendEvent(StateMachineConfig.Events.EOD);

        Thread.sleep(8000);

        assertThat(stateMachine.getState().getId()).isEqualTo(StateMachineConfig.States.IDLE);
        verify(dspApprovedGuard, times(3)).evaluate(any());
        verify(tradesInFlightGuard, times(4)).evaluate(any());
        verify(swpPointApprovedGuard, times(2)).evaluate(any());
        stateMachine.stop();

        verify(eod1runAction).execute(any());
        verify(eod1CompleteAction).execute(any());
        verify(eod2runAction).execute(any());
        verify(ledgerRunAction).execute(any());
        verify(dateRollRunAction).execute(any());
    }

    @Test
    void shouldRunWhenNoTradesInFlightBeforeDspApproval() throws Exception {
        stateMachine.start();

        when(dspApprovedGuard.evaluate(any())).thenReturn(false, false, true);
        when(tradesInFlightGuard.evaluate(any())).thenReturn(false, true);
        when(swpPointApprovedGuard.evaluate(any())).thenReturn(true);
        stateMachine.sendEvent(StateMachineConfig.Events.EOD);

        Thread.sleep(6000);

        assertThat(stateMachine.getState().getId()).isEqualTo(StateMachineConfig.States.IDLE);
        verify(dspApprovedGuard, times(3)).evaluate(any());
        verify(tradesInFlightGuard, times(2)).evaluate(any());
        verify(swpPointApprovedGuard, times(1)).evaluate(any());
        stateMachine.stop();

        verify(eod1runAction).execute(any());
        verify(eod1CompleteAction).execute(any());
        verify(eod2runAction).execute(any());
        verify(ledgerRunAction).execute(any());
        verify(dateRollRunAction).execute(any());
    }

}