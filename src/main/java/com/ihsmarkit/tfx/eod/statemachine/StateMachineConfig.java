package com.ihsmarkit.tfx.eod.statemachine;

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
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.IDLE;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.INIT;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.LEDGER_COMPLETE;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.LEDGER_RUN;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.NO_DSP_NO_TRADES;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.NO_DSP_NO_TRADES_DELAY;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.NO_DSP_TRADES;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.NO_DSP_TRADES_DELAY;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.READY;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.SWP_PNT_APPROVED;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.SWP_PNT_CHECK;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.SWP_PNT_DELAY;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.SWP_PNT_NOTAPPROVED;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings({
    "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR",
    "PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS",
    "SIC_INNER_SHOULD_BE_STATIC_ANON"
})
@EnableStateMachine
public class StateMachineConfig extends EnumStateMachineConfigurerAdapter<StateMachineConfig.States, StateMachineConfig.Events> {
    public enum States { IDLE, READY, INIT, EOD1, NO_DSP_NO_TRADES, NO_DSP_NO_TRADES_DELAY, DSP_CHECK, DSP_NO_TRADES, DSP_NO_TRADES_DELAY, NO_DSP_TRADES,
        NO_DSP_TRADES_DELAY, EOD1_READY, EOD1_RUN, EOD1_COMPLETE, EOD2, SWP_PNT_CHECK, SWP_PNT_NOTAPPROVED, SWP_PNT_APPROVED,
        SWP_PNT_DELAY, EOD2_RUN, EOD2_COMPLETE, LEDGER_RUN, LEDGER_COMPLETE, DATE_ROLL_RUN
    }
    public enum Events { EOD, STOP }

    private static final int WAIT_TIME = 1000;

    @Autowired
    @Qualifier("dspApprovedGuard")
    private Guard<States, Events> dspApprovedGuard;

    @Autowired
    @Qualifier("tradesInFlightGuard")
    private Guard<States, Events> tradesInFlightGuard;

    @Autowired
    @Qualifier("swpPointApprovedGuard")
    private Guard<States, Events> swpPointApprovedGuard;

    @Autowired
    @Qualifier("initAction")
    private Action<States, Events> initAction;

    @Autowired
    @Qualifier("resetErrorAction")
    private Action<States, Events> resetErrorActionBean;

    @Autowired
    @Qualifier("eod1runAction")
    private Action<States, Events> eod1runAction;

    @Autowired
    @Qualifier("eod1CompleteAction")
    private Action<States, Events> eod1CompleteAction;

    @Autowired
    @Qualifier("eod2runAction")
    private Action<States, Events> eod2runAction;

    @Autowired
    @Qualifier("ledgerRunAction")
    private Action<States, Events> ledgerRunAction;

    @Autowired
    @Qualifier("dateRollRunAction")
    private Action<States, Events> dateRollRunAction;


    @Override
    public void configure(final StateMachineStateConfigurer<States, Events> states) throws Exception {
        states.
            withStates()
            .initial(IDLE)
            .choice(EOD1_RUN)
            .choice(EOD2_RUN)
            .choice(LEDGER_RUN)
            .states(Set.of(INIT, READY, EOD1, EOD1_COMPLETE, EOD2, EOD2_COMPLETE, LEDGER_COMPLETE, DATE_ROLL_RUN))
            .and().withStates()
                .parent(EOD1)
                .initial(DSP_CHECK)
                .choice(NO_DSP_NO_TRADES)
                .choice(DSP_NO_TRADES)
                .choice(NO_DSP_TRADES)
                .states(Set.of(EOD1_READY, NO_DSP_TRADES_DELAY, DSP_NO_TRADES_DELAY, NO_DSP_NO_TRADES_DELAY))
            .and().withStates()
                .parent(EOD2)
                .initial(SWP_PNT_CHECK)
                .choice(SWP_PNT_NOTAPPROVED)
                .states(Set.of(SWP_PNT_APPROVED, SWP_PNT_DELAY));
    }

    @Override
    public void configure(final StateMachineTransitionConfigurer<States, Events> transitions) throws Exception {
        transitions
            .withExternal()
                .source(EOD1).target(IDLE).event(Events.STOP)
            .and().withExternal()
                .source(EOD2).target(IDLE).event(Events.STOP)
            .and().withExternal()
                .source(IDLE).target(READY).action(resetErrorActionBean)
            .and().withExternal()
                .source(READY).target(INIT).event(Events.EOD)
            .and().withExternal()
                .source(INIT).target(EOD1).action(initAction)
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
                .source(EOD2_COMPLETE).target(LEDGER_RUN).action(ledgerRunAction)
            .and().withChoice()
                .source(LEDGER_RUN)
                .first(LEDGER_COMPLETE, noErrorGuard())
                .last(IDLE)
            .and().withExternal()
                .source(LEDGER_COMPLETE).target(DATE_ROLL_RUN).action(dateRollRunAction)
            .and().withExternal()
                .source(DATE_ROLL_RUN).target(IDLE);
    }

    @Bean
    public Guard<States, Events> noErrorGuard() {
        return new Guard<States, Events>() {
            @Override
            public boolean evaluate(final StateContext<States, Events> context) {
                return !context.getStateMachine().hasStateMachineError();
            }
        };
    }

    @Bean
    public Action<StateMachineConfig.States, StateMachineConfig.Events> resetErrorAction() {
        return new Action<>() {
            @Override
            public void execute(final StateContext<StateMachineConfig.States, StateMachineConfig.Events> context) {
                context.getStateMachine().setStateMachineError(null);
            }
        };
    }

}
