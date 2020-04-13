package com.ihsmarkit.tfx.eod.statemachine;

import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.DATE_ROLL;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.DATE_ROLL_RUN;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.DSP_CHECK;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.DSP_NO_TRADES;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.DSP_NO_TRADES_DELAY;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.EOD1;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.EOD1_COMPLETE;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.EOD1_READY;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.EOD1_RUN;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.EOD2;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.EOD2_COMPLETE;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.EOD2_RUN;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.EOD_DATE_CHECK;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.EOD_PREMATURE;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.IDLE;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.INIT;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.NO_DSP_NO_TRADES;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.NO_DSP_NO_TRADES_DELAY;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.NO_DSP_TRADES;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.NO_DSP_TRADES_DELAY;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.READY;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.SWP_PNT_APPROVED;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.SWP_PNT_CHECK;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.SWP_PNT_DELAY;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.SWP_PNT_NOTAPPROVED;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;

import lombok.RequiredArgsConstructor;

@EnableStateMachine
@RequiredArgsConstructor
public class StateMachineConfig extends EnumStateMachineConfigurerAdapter<StateMachineConfig.States, StateMachineConfig.Events> {
    public enum States { IDLE, READY, INIT, EOD1, NO_DSP_NO_TRADES, NO_DSP_NO_TRADES_DELAY, DSP_CHECK, DSP_NO_TRADES, DSP_NO_TRADES_DELAY, NO_DSP_TRADES,
        NO_DSP_TRADES_DELAY, EOD1_READY, EOD1_RUN, EOD1_COMPLETE, EOD2, SWP_PNT_CHECK, SWP_PNT_NOTAPPROVED, SWP_PNT_APPROVED,
        SWP_PNT_DELAY, EOD2_RUN, EOD2_COMPLETE, DATE_ROLL, DATE_ROLL_RUN, EOD_DATE_CHECK, EOD_PREMATURE
    }
    public enum Events { EOD, STOP }

    private static final int WAIT_TIME = 1000;

    @Qualifier("dspApprovedGuard")
    private final Guard<States, Events> dspApprovedGuard;

    @Qualifier("tradesInFlightGuard")
    private final Guard<States, Events> tradesInFlightGuard;

    @Qualifier("eodDateCheckGuard")
    private final Guard<States, Events> eodDateCheckGuard;

    @Qualifier("swpPointApprovedGuard")
    private final Guard<States, Events> swpPointApprovedGuard;

    @Qualifier("initAction")
    private final Action<States, Events> initAction;

    @Qualifier("eodPrematureAction")
    private final Action<States, Events> eodPrematureAction;

    @Qualifier("resetErrorAction")
    private final Action<States, Events> resetErrorActionBean;

    @Autowired
    @Qualifier("eod1runAction")
    private final Action<States, Events> eod1runAction;

    @Qualifier("eod1CompleteAction")
    private final Action<States, Events> eod1CompleteAction;

    @Qualifier("eod2CompleteAction")
    private final Action<States, Events> eod2CompleteAction;

    @Qualifier("eod2runAction")
    private final Action<States, Events> eod2runAction;

    @Qualifier("dateRollRunAction")
    private final Action<States, Events> dateRollRunAction;

    @Override
    public void configure(final StateMachineConfigurationConfigurer<States, Events> config) throws Exception {
        config.withConfiguration().autoStartup(true);
    }

    @Override
    public void configure(final StateMachineStateConfigurer<States, Events> states) throws Exception {
        states.
            withStates()
            .initial(IDLE)
            .choice(EOD1_RUN)
            .choice(EOD2_RUN)
            .state(INIT)
            .choice(EOD_DATE_CHECK)
            .state(EOD_PREMATURE)
            .state(READY)
            .state(EOD1)
            .state(EOD1_COMPLETE, Events.STOP)
            .state(EOD2)
            .state(EOD2_COMPLETE, Events.STOP)
            .state(DATE_ROLL, Events.STOP)
            .state(DATE_ROLL_RUN, Events.STOP)
            .and().withStates()
                .parent(EOD1)
                .initial(DSP_CHECK)
                .state(NO_DSP_TRADES_DELAY)
                .state(DSP_NO_TRADES_DELAY)
                .state(NO_DSP_NO_TRADES_DELAY)
                .state(EOD1_READY, Events.STOP)
                .choice(NO_DSP_NO_TRADES)
                .choice(DSP_NO_TRADES)
                .choice(NO_DSP_TRADES)
            .and().withStates()
                .parent(EOD2)
                .initial(SWP_PNT_CHECK)
                .choice(SWP_PNT_NOTAPPROVED)
                .state(SWP_PNT_APPROVED, Events.STOP)
                .state(SWP_PNT_DELAY);
    }

    @Override
    public void configure(final StateMachineTransitionConfigurer<States, Events> transitions) throws Exception {
        transitions
            .withExternal()
                .source(SWP_PNT_DELAY).target(IDLE).event(Events.STOP)
            .and().withExternal()
                .source(DSP_NO_TRADES_DELAY).target(IDLE).event(Events.STOP)
            .and().withExternal()
                .source(NO_DSP_NO_TRADES_DELAY).target(IDLE).event(Events.STOP)
            .and().withExternal()
                .source(NO_DSP_TRADES_DELAY).target(IDLE).event(Events.STOP)
            .and().withExternal()
                .source(IDLE).target(READY).action(resetErrorActionBean)
            .and().withExternal()
                .source(READY).target(INIT).event(Events.EOD)
            .and().withExternal()
                .source(INIT).target(EOD_DATE_CHECK).action(initAction)
            .and().withChoice()
                .source(EOD_DATE_CHECK)
                .first(EOD1, eodDateCheckGuard)
                .last(EOD_PREMATURE)
            .and().withExternal()
                .source(EOD_PREMATURE).target(IDLE).action(eodPrematureAction)
            .and().withExternal()
                .source(DSP_CHECK).target(NO_DSP_NO_TRADES)
            .and().withChoice()
                .source(NO_DSP_NO_TRADES)
                .first(DSP_NO_TRADES, dspApprovedGuard)
                .then(NO_DSP_TRADES, tradesInFlightGuard)
                .last(NO_DSP_NO_TRADES_DELAY)
            .and().withChoice()
                .source(DSP_NO_TRADES)
                .first(EOD1_READY, tradesInFlightGuard)
                .last(DSP_NO_TRADES_DELAY)
            .and().withChoice()
                .source(NO_DSP_TRADES)
                .first(EOD1_READY, dspApprovedGuard)
                .last(NO_DSP_TRADES_DELAY)
            .and().withExternal()
                .source(NO_DSP_TRADES_DELAY).target(NO_DSP_TRADES).timer(WAIT_TIME)
            .and().withExternal()
                .source(DSP_NO_TRADES_DELAY).target(DSP_NO_TRADES).timer(WAIT_TIME)
            .and().withExternal()
                .source(NO_DSP_NO_TRADES_DELAY).target(NO_DSP_NO_TRADES).timer(WAIT_TIME)
            .and().withExternal()
                .source(EOD1_READY).target(EOD1_RUN).action(eod1runAction)
            .and().withChoice()
                .source(EOD1_RUN)
                .first(EOD1_COMPLETE, noErrorGuard())
                .last(IDLE)
            .and().withExternal()
                .source(EOD1_COMPLETE).target(EOD2).action(eod1CompleteAction)
            .and().withExternal()
                .source(SWP_PNT_CHECK).target(SWP_PNT_NOTAPPROVED)
            .and().withChoice()
                .source(SWP_PNT_NOTAPPROVED)
                .first(SWP_PNT_APPROVED, swpPointApprovedGuard)
                .last(SWP_PNT_DELAY)
            .and().withExternal()
                .source(SWP_PNT_DELAY).target(SWP_PNT_NOTAPPROVED).timer(WAIT_TIME)
            .and().withExternal()
                .source(SWP_PNT_APPROVED).target(EOD2_RUN).action(eod2runAction)
            .and().withChoice()
                .source(EOD2_RUN)
                .first(EOD2_COMPLETE, noErrorGuard())
                .last(IDLE)
            .and().withExternal()
                .source(EOD2_COMPLETE).target(DATE_ROLL).action(eod2CompleteAction)
            .and().withExternal()
                .source(DATE_ROLL).target(DATE_ROLL_RUN).action(dateRollRunAction)
            .and().withExternal()
                .source(DATE_ROLL_RUN).target(IDLE);
    }

    @Bean
    public static Guard<States, Events> noErrorGuard() {
        return context -> !context.getStateMachine().hasStateMachineError();
    }

    @Bean
    public static Action<StateMachineConfig.States, StateMachineConfig.Events> resetErrorAction() {
        return context -> context.getStateMachine().setStateMachineError(null);
    }

}
