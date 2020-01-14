package com.ihsmarkit.tfx.eod.statemachine;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class StateWaitingListener extends StateMachineListenerAdapter<StateMachineConfig.States, StateMachineConfig.Events> {
    private final CountDownLatch latch = new CountDownLatch(1);
    private final StateMachineConfig.States state;

    public StateWaitingListener(final StateMachineConfig.States state) {
        this.state = state;
    }

    @Override
    public void stateChanged(
        final State<StateMachineConfig.States, StateMachineConfig.Events> from,
        final State<StateMachineConfig.States, StateMachineConfig.Events> to
    ) {
        if (latch.getCount() == 1L && to.getId() == state) {
            latch.countDown();
        }
    }
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
    public void await(
        final StateMachine<StateMachineConfig.States, StateMachineConfig.Events> stateMachine,
        final long seconds
    ) throws InterruptedException {

        if (stateMachine.getState().getId() == state) {
            latch.countDown();
        } else {
            latch.await(seconds, TimeUnit.SECONDS);
        }
    }
}
