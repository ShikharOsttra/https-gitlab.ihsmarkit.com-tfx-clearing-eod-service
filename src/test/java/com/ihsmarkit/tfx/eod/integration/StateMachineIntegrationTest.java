package com.ihsmarkit.tfx.eod.integration;

import static com.ihsmarkit.tfx.core.dl.entity.eod.EodStage.DSP_APPROVED;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineActionsConfig.BUSINESS_DATE_ATTRIBUTE;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.DSP_NO_TRADES_DELAY;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.EOD1;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.EOD2;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.NO_DSP_NO_TRADES_DELAY;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.NO_DSP_TRADES_DELAY;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.READY;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.States.SWP_PNT_DELAY;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateMachine;
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
public class StateMachineIntegrationTest {
    @Autowired
    private StateMachine<StateMachineConfig.States, StateMachineConfig.Events> stateMachine;

    @Autowired
    private EodStatusRepository eodStatusRepository;

    @Test
    @DatabaseSetup({"/common/currency.xml", "/common/participants.xml", "/statemachine/business_date_2019_1_1.xml"})
    void shouldWaitDSP() throws InterruptedException {

        stateMachine.sendEvent(StateMachineConfig.Events.EOD);
        Thread.sleep(2000);

        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE_ATTRIBUTE)).isEqualTo(LocalDate.of(2019, 1, 1));
        assertThat(stateMachine.getState().getIds()).containsOnly(EOD1, NO_DSP_TRADES_DELAY);

    }

    @Test
    @DatabaseSetup({"/common/currency.xml", "/common/participants.xml", "/statemachine/business_date_2019_1_1.xml"})
    void shouldWaitDSPAndProceedToSwpPnt() throws InterruptedException {

        stateMachine.sendEvent(StateMachineConfig.Events.EOD);
        Thread.sleep(2000);

        eodStatusRepository.save(
            EodStatusEntity.builder()
                .id(new EodStatusCompositeId(DSP_APPROVED, LocalDate.of(2019, 1, 1)))
                .timestamp(LocalDateTime.now())
                .build()
        );
        Thread.sleep(2000);

        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE_ATTRIBUTE)).isEqualTo(LocalDate.of(2019, 1, 1));
        assertThat(stateMachine.getState().getIds()).containsOnly(EOD2, SWP_PNT_DELAY);
    }

    @Test
    @DatabaseSetup({"/common/currency.xml", "/common/participants.xml", "/statemachine/business_date_2019_1_2.xml"})
    void shouldWaitTradesAndDSP() throws InterruptedException {

        stateMachine.sendEvent(StateMachineConfig.Events.EOD);

        Thread.sleep(5000);

        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE_ATTRIBUTE)).isEqualTo(LocalDate.of(2019, 1, 2));
        assertThat(stateMachine.getState().getIds()).containsOnly(EOD1, NO_DSP_NO_TRADES_DELAY);

    }

    @Test
    @DatabaseSetup({"/common/currency.xml", "/common/participants.xml", "/statemachine/business_date_2019_1_3.xml"})
    void shouldWaitTrades() throws InterruptedException {

        stateMachine.sendEvent(StateMachineConfig.Events.EOD);
        Thread.sleep(2000);

        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE_ATTRIBUTE)).isEqualTo(LocalDate.of(2019, 1, 3));
        assertThat(stateMachine.getState().getIds()).containsOnly(EOD1, DSP_NO_TRADES_DELAY);

    }

    @Test
    @DatabaseSetup({"/common/currency.xml", "/common/participants.xml", "/statemachine/business_date_2019_1_4.xml"})
    void shouldWaitSwapPoints() throws InterruptedException {

        stateMachine.sendEvent(StateMachineConfig.Events.EOD);

        Thread.sleep(2000);

        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE_ATTRIBUTE)).isEqualTo(LocalDate.of(2019, 1, 4));
        assertThat(stateMachine.getState().getIds()).containsOnly(EOD2, SWP_PNT_DELAY);

    }

    @Test
    @DatabaseSetup({"/common/currency.xml", "/common/participants.xml", "/statemachine/business_date_2019_1_5.xml"})
    @ExpectedDatabase(value = "/statemachine/business_date_2019_1_5-expected.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void shouldRunFullCycle() throws InterruptedException {

        stateMachine.sendEvent(StateMachineConfig.Events.EOD);

        Thread.sleep(3000);

        assertThat(stateMachine.getExtendedState().getVariables().get(BUSINESS_DATE_ATTRIBUTE)).isEqualTo(LocalDate.of(2019, 1, 5));
        assertThat(stateMachine.getState().getIds()).containsOnly(READY);

    }

    @AfterEach
    void stopMachine() throws InterruptedException {

        stateMachine.sendEvent(StateMachineConfig.Events.STOP);
        Thread.sleep(1000);

    }
}
