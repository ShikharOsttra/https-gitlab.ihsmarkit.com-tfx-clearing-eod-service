package com.ihsmarkit.tfx.eod.statemachine;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;

@FunctionalInterface
public interface EodGuard extends Guard<StateMachineConfig.States, StateMachineConfig.Events> {

    @Override
    default boolean evaluate(final StateContext<StateMachineConfig.States, StateMachineConfig.Events> context) {
        return evaluate(EodContext.of(context));
    }

    boolean evaluate(EodContext context);
}
