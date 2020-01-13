package com.ihsmarkit.tfx.eod.statemachine;

import static com.ihsmarkit.tfx.eod.statemachine.StateMachineActionsConfig.BUSINESS_DATE_ATTRIBUTE;

import java.time.LocalDate;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;

@FunctionalInterface
public interface EodGuard extends Guard<StateMachineConfig.States, StateMachineConfig.Events> {

    @Override
    default boolean evaluate(final StateContext<StateMachineConfig.States, StateMachineConfig.Events> context) {
        final LocalDate date = (LocalDate) context.getExtendedState().getVariables().get(BUSINESS_DATE_ATTRIBUTE);
        return evaluate(date);
    }

    boolean evaluate(LocalDate businessDate);
}
