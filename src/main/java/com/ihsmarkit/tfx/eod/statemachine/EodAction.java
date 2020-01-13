package com.ihsmarkit.tfx.eod.statemachine;

import static com.ihsmarkit.tfx.core.domain.type.SystemParameters.BUSINESS_DATE;

import java.time.LocalDate;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

@FunctionalInterface
public interface EodAction extends Action<StateMachineConfig.States, StateMachineConfig.Events> {

    @Override
    default void execute(final StateContext<StateMachineConfig.States, StateMachineConfig.Events> context) {
        final LocalDate businessDate = (LocalDate) context.getExtendedState().getVariables().get(BUSINESS_DATE);
        execute(businessDate);
    }

    void execute(LocalDate date);

}
