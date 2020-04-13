package com.ihsmarkit.tfx.eod.statemachine;

import static com.ihsmarkit.tfx.eod.statemachine.StateMachineActionsConfig.BUSINESS_DATE_ATTRIBUTE;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineActionsConfig.WALLCLOCK_TIMESTAMP_ATTRIBUTE;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.statemachine.StateContext;

import lombok.Data;

@Data
public class EodContext {

    private final LocalDate businessDate;
    private final LocalDateTime timestamp;

    public static EodContext of(final StateContext<StateMachineConfig.States, StateMachineConfig.Events> context) {

        final Map<Object, Object> variables = context.getExtendedState().getVariables();

        final LocalDate businessDate = (LocalDate) variables.get(BUSINESS_DATE_ATTRIBUTE);
        final LocalDateTime timestamp = (LocalDateTime) variables.get(WALLCLOCK_TIMESTAMP_ATTRIBUTE);

        return new EodContext(businessDate, timestamp);
    }
}
