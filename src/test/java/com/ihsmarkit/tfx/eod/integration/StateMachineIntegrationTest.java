package com.ihsmarkit.tfx.eod.integration;

import static com.ihsmarkit.tfx.core.dl.entity.eod.EodStage.DSP_APPROVED;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineActionsConfig.BUSINESS_DATE_ATTRIBUTE;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.Events.EOD;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.Events.STOP;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.DSP_NO_TRADES_DELAY;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.EOD1;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.EOD2;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.NO_DSP_NO_TRADES_DELAY;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.NO_DSP_TRADES_DELAY;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.READY;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.SWP_PNT_DELAY;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;
import org.springframework.statemachine.transition.Transition;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodStatusCompositeId;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodStatusEntity;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodStatusRepository;
import com.ihsmarkit.tfx.eod.config.EOD1JobConfig;
import com.ihsmarkit.tfx.eod.config.EOD2JobConfig;
import com.ihsmarkit.tfx.eod.config.RollBusinessDateJobConfig;
import com.ihsmarkit.tfx.eod.statemachine.StateMachineActionsConfig;
import com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig;
import com.ihsmarkit.tfx.test.utils.db.DbUnitTestListeners;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ExtendWith(SpringExtension.class)
@DbUnitTestListeners
@DatabaseTearDown("/common/tearDown.xml")
@ContextHierarchy({
    @ContextConfiguration(classes = IntegrationTestConfig.class),
    @ContextConfiguration(classes = {
        EOD1JobConfig.class,
        EOD2JobConfig.class,
        RollBusinessDateJobConfig.class,
        StateMachineConfig.class,
        StateMachineActionsConfig.class
    })}
)
@TestPropertySource("classpath:/application.properties")
@SuppressFBWarnings("MDM_THREAD_YIELD")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StateMachineIntegrationTest {

    final static LocalDate JAN_1 = LocalDate.of(2019, 1, 1);
    final static LocalDate JAN_2 = JAN_1.plusDays(1);
    final static LocalDate JAN_3 = JAN_1.plusDays(2);
    final static LocalDate JAN_4 = JAN_1.plusDays(3);
    final static LocalDate JAN_5 = JAN_1.plusDays(4);

    @Autowired
    private StateMachine<StateMachineConfig.States, StateMachineConfig.Events> stateMachine;

    @Autowired
    private EodStatusRepository eodStatusRepository;

    @Test
    @DatabaseSetup({"/common/currency.xml", "/common/participants.xml", "/statemachine/business_date_2019_1_1.xml"})
    void shouldWaitDSP() throws Exception {

        testPlanBuilder()
            .stateMachine(stateMachine)
            .defaultAwaitTime(20)
            .step()
                .expectState(READY)
            .and().step()
                .sendEvent(EOD)
                .expectVariableWith(hasEntry(is(BUSINESS_DATE), is(JAN_1)))
                .expectStates(EOD1, NO_DSP_TRADES_DELAY)
            .and().build().test();

        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE_ATTRIBUTE)).isEqualTo(JAN_1);
        assertThat(stateMachine.getState().getIds()).containsOnly(EOD1, NO_DSP_TRADES_DELAY);

    }

    @Test
    @DatabaseSetup({"/common/currency.xml", "/common/participants.xml", "/statemachine/business_date_2019_1_1.xml"})
    void shouldWaitDSPAndProceedToSwpPnt() throws InterruptedException {

        stateMachine.sendEvent(StateMachineConfig.Events.EOD);
        Thread.sleep(2000);

        eodStatusRepository.save(
            EodStatusEntity.builder()
                .id(new EodStatusCompositeId(DSP_APPROVED, JAN_1))
                .timestamp(LocalDateTime.now())
                .build()
        );
        Thread.sleep(2000);

        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE_ATTRIBUTE)).isEqualTo(JAN_1);
        assertThat(stateMachine.getState().getIds()).containsOnly(EOD2, SWP_PNT_DELAY);
    }

    @Test
    @DatabaseSetup({"/common/currency.xml", "/common/participants.xml", "/statemachine/business_date_2019_1_2.xml"})
    void shouldWaitTradesAndDSP() throws Exception {

        testPlanBuilder()
            .defaultAwaitTime(20)
            .stateMachine(stateMachine)
            .step()
                .expectState(READY)
            .and().step()
                .sendEvent(EOD)
                .expectVariableWith(hasEntry(is(BUSINESS_DATE), is(JAN_2)))
                .expectStates(EOD1, NO_DSP_NO_TRADES_DELAY)
            .and().build().test();

        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE)).isEqualTo(JAN_2);

        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE_ATTRIBUTE)).isEqualTo(JAN_2);
        assertThat(stateMachine.getState().getIds()).containsOnly(EOD1, NO_DSP_NO_TRADES_DELAY);

    }

    @Test
    @DatabaseSetup({"/common/currency.xml", "/common/participants.xml", "/statemachine/business_date_2019_1_3.xml"})
    void shouldWaitTrades() throws Exception {
        testPlanBuilder()
            .defaultAwaitTime(20)
            .stateMachine(stateMachine)
            .step()
                .expectState(READY)
            .and().step()
                .sendEvent(EOD)
                .expectVariableWith(hasEntry(is(BUSINESS_DATE), is(JAN_3)))
                .expectStates(EOD1, DSP_NO_TRADES_DELAY)
            .and().build().test();

        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE)).isEqualTo(JAN_3);

        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE_ATTRIBUTE)).isEqualTo(JAN_3);
        assertThat(stateMachine.getState().getIds()).containsOnly(EOD1, DSP_NO_TRADES_DELAY);

    }

    @Test
    @DatabaseSetup({"/common/currency.xml", "/common/participants.xml", "/statemachine/business_date_2019_1_4.xml"})
    void shouldWaitSwapPoints() throws Exception {

        testPlanBuilder()
            .defaultAwaitTime(20)
            .stateMachine(stateMachine)
            .step()
                .expectState(READY)
            .and().step()
                .sendEvent(EOD)
                .expectVariableWith(hasEntry(is(BUSINESS_DATE), is(JAN_4)))
                .expectStates(EOD2, SWP_PNT_DELAY)
            .and().build().test();

        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE)).isEqualTo(JAN_4);

        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE_ATTRIBUTE)).isEqualTo(JAN_4);
        assertThat(stateMachine.getState().getIds()).containsOnly(EOD2, SWP_PNT_DELAY);

    }

    @Test
    @DatabaseSetup({"/common/currency.xml", "/common/participants.xml", "/statemachine/business_date_2019_1_5.xml"})
    @ExpectedDatabase(value = "/statemachine/business_date_2019_1_5-expected.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void shouldRunFullCycle() throws Exception {
        testPlanBuilder()
            .defaultAwaitTime(20)
            .stateMachine(stateMachine)
            .step()
                .expectState(READY)
            .and().step()
                .sendEvent(EOD)
                .expectVariableWith(hasEntry(is(BUSINESS_DATE), is(JAN_5)))
                .expectState(READY)
            .and().build().test();
        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE)).isEqualTo(JAN_5);

    }

    private StateMachineTestPlanBuilder<StateMachineConfig.States, StateMachineConfig.Events> testPlanBuilder() {
        return StateMachineTestPlanBuilder.<StateMachineConfig.States, StateMachineConfig.Events>builder();
    }

        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE_ATTRIBUTE)).isEqualTo(JAN_5);
        assertThat(stateMachine.getState().getIds()).containsOnly(READY);

        final ReadyWaitingListener listener = new ReadyWaitingListener();
        stateMachine.addStateListener(listener);
        stateMachine.sendEvent(STOP);
        listener.await(stateMachine, 5);

    }

    @AfterEach
    void cooldown() throws InterruptedException {
        Thread.sleep(2000);

    }


    @BeforeAll
    void addListener() {
        stateMachine.addStateListener(new StateMachineListenerAdapter<>() {
            @Override
            public void stateChanged(State<StateMachineConfig.States, StateMachineConfig.Events> from, State<StateMachineConfig.States, StateMachineConfig.Events> to) {
                System.out.println("*****[stateChanged] " + from + " " + to);
            }

            @Override
            public void stateEntered(State<StateMachineConfig.States, StateMachineConfig.Events> state) {
                System.out.println("*****[stateEntered] " + state);
            }

            @Override
            public void stateExited(State<StateMachineConfig.States, StateMachineConfig.Events> state) {
                System.out.println("*****[stateExited] " + state);
            }

            @Override
            public void eventNotAccepted(Message<StateMachineConfig.Events> event) {
                System.out.println("*****[eventNotAccepted] " + event);
            }

            @Override
            public void transition(Transition<StateMachineConfig.States, StateMachineConfig.Events> transition) {
                System.out.println("*****[transition] " + transition);

            }

            @Override
            public void transitionStarted(Transition<StateMachineConfig.States, StateMachineConfig.Events> transition) {
                System.out.println("*****[transitionStarted] " + transition);

            }

            @Override
            public void transitionEnded(Transition<StateMachineConfig.States, StateMachineConfig.Events> transition) {
                System.out.println("*****[transitionEnded] " + transition);

            }

            @Override
            public void stateMachineStarted(StateMachine<StateMachineConfig.States, StateMachineConfig.Events> stateMachine) {
                System.out.println("*****[stateMachineStarted] " + stateMachine);

            }

            @Override
            public void stateMachineStopped(StateMachine<StateMachineConfig.States, StateMachineConfig.Events> stateMachine) {
                System.out.println("*****[stateMachineStopped] " + stateMachine);

            }

            @Override
            public void stateMachineError(StateMachine<StateMachineConfig.States, StateMachineConfig.Events> stateMachine, Exception exception) {
                System.out.println("*****[stateMachineError] " + stateMachine + " " + exception.getClass().getName() + " " +exception.getMessage());

            }

            @Override
            public void extendedStateChanged(Object key, Object value) {
                System.out.println("*****[extendedStateChanged] " + key + " " + value);

            }

            @Override
            public void stateContext(StateContext<StateMachineConfig.States, StateMachineConfig.Events> stateContext) {
                System.out.println("*****[stateContext] " + stateContext);

            }
        });

    }

    private static class ReadyWaitingListener extends StateMachineListenerAdapter<StateMachineConfig.States, StateMachineConfig.Events> {
        private CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void stateChanged(State<StateMachineConfig.States, StateMachineConfig.Events> from, State<StateMachineConfig.States, StateMachineConfig.Events> to) {
            if (latch.getCount()==1L) if (to.getId() == READY) {
                System.out.println("++++++++++ " + to.getStates().stream().map(Object::toString).collect(joining("--")) + " +++++ " + System.currentTimeMillis());
                latch.countDown();
            } else {
                System.out.println("==== " + to.getStates().stream().map(Object::toString).collect(joining("--")) + System.currentTimeMillis());

            }
        }

        public void await(StateMachine<StateMachineConfig.States, StateMachineConfig.Events> stateMachine, long seconds) throws InterruptedException {
            if (stateMachine.getState().getId() == READY) latch.countDown();
            {
                System.out.println(">>>>>>>>"+ System.currentTimeMillis());

                latch.await(seconds, TimeUnit.SECONDS);
                System.out.println("<<<<<<<<"+ System.currentTimeMillis());

            }
        }
    }
}
