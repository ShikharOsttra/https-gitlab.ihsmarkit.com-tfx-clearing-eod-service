package com.ihsmarkit.tfx.eod.config;

import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.Events.EOD;

import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.statemachine.StateMachine;

import com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//@SuppressFBWarnings("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
@RequiredArgsConstructor
public class EodQuartzJob extends QuartzJobBean {

    private StateMachine<StateMachineConfig.States, StateMachineConfig.Events> stateMachine;

    @Override
    @SneakyThrows
    protected void executeInternal(final JobExecutionContext context) {
        stateMachine.sendEvent(EOD);
    }

}