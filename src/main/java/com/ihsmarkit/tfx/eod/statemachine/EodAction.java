package com.ihsmarkit.tfx.eod.statemachine;

import static com.ihsmarkit.tfx.eod.statemachine.StateMachineActionsConfig.BUSINESS_DATE_ATTRIBUTE;

import java.time.LocalDate;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

@FunctionalInterface
public interface EodAction extends Action<StateMachineConfig.States, StateMachineConfig.Events> {

    @Override
    default void execute(final StateContext<StateMachineConfig.States, StateMachineConfig.Events> context) {
        final LocalDate businessDate = (LocalDate) context.getExtendedState().getVariables().get(BUSINESS_DATE_ATTRIBUTE);
        execute(businessDate);
    }

    void execute(LocalDate date);

}
