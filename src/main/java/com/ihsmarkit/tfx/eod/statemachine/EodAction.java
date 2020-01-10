package com.ihsmarkit.tfx.eod.statemachine;

import static com.ihsmarkit.tfx.core.domain.type.SystemParameters.BUSINESS_DATE;

import java.time.LocalDate;

import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.SneakyThrows;

@FunctionalInterface
public interface EodAction extends Action<StateMachineConfig.States, StateMachineConfig.Events> {

    @SneakyThrows
    @Override
    default void execute(final StateContext<StateMachineConfig.States, StateMachineConfig.Events> context) {
        final LocalDate businessDate = (LocalDate) context.getExtendedState().getVariables().get(BUSINESS_DATE);
        execute(businessDate);
    }

    void execute(LocalDate date)
        throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException, JobParametersInvalidException;
}
