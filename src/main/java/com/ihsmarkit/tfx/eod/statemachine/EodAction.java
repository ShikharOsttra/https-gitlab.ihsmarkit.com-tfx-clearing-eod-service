package com.ihsmarkit.tfx.eod.statemachine;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

@FunctionalInterface
public interface EodAction extends Action<StateMachineConfig.States, StateMachineConfig.Events> {

    @Override
    default void execute(final StateContext<StateMachineConfig.States, StateMachineConfig.Events> context) {
        try {
            execute(EodContext.of(context));
        } catch (RuntimeException e) {
            context.getStateMachine().setStateMachineError(e);
        }
    }

    void execute(EodContext context);

}
