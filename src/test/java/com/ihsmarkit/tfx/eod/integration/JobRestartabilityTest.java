package com.ihsmarkit.tfx.eod.integration;

import static com.ihsmarkit.tfx.alert.client.domain.type.AlertEventName.EOD_1_COMPLETED;
import static com.ihsmarkit.tfx.alert.client.domain.type.AlertEventName.EOD_2_COMPLETED;
import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aCurrencyPairEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aFxSpotProductEntity;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.COLLATERAL_BALANCE_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.COLLATERAL_LIST_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.DAILY_MARKET_DATA_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD1_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD2_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD_COMPLETE_NOTIFY_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.LEDGER_CLEANUP_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.MARGIN_COLLATERAL_EXCESS_OR_DEFICIENCY;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.MTM_TRADES_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.NET_TRADES_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.NET_TRANSACTION_DIARY_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.OPEN_POSITIONS_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.REBALANCE_POSITIONS_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.ROLL_BUSINESS_DATE_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.ROLL_BUSINESS_DATE_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.ROLL_POSITIONS_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.SOD_TRANSACTION_DIARY_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.SWAP_PNL_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.TOTAL_VM_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.TRADE_TRANSACTION_DIARY_LEDGER_STEP_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.TRANSACTION_DIARY_RECORD_DATE_SET_STEP_NAME;
import static com.ihsmarkit.tfx.eod.statemachine.StateMachineConfig.Events.EOD;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.batch.core.BatchStatus.COMPLETED;
import static org.springframework.batch.core.BatchStatus.FAILED;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.apache.commons.lang3.tuple.Pair;
import org.dbunit.database.DatabaseDataSourceConnection;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.xml.FlatXmlProducer;
import org.dbunit.operation.DatabaseOperation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.statemachine.StateMachine;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.xml.sax.InputSource;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.ihsmarkit.tfx.alert.client.domain.NewAlert;
import com.ihsmarkit.tfx.alert.client.domain.type.AlertEventName;
import com.ihsmarkit.tfx.alert.client.jms.AlertSender;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.EODThresholdFutureValueEntity;
import com.ihsmarkit.tfx.core.dl.entity.FxSpotProductEntity;
import com.ihsmarkit.tfx.core.dl.entity.marketdata.DailySettlementPriceEntity;
import com.ihsmarkit.tfx.core.dl.repository.EODThresholdFutureValueRepository;
import com.ihsmarkit.tfx.core.dl.repository.marketdata.DailySettlementPriceRepository;
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
@ContextConfiguration(classes = {
    IntegrationTestConfig.class,
    EOD1JobConfig.class,
    EOD2JobConfig.class,
    RollBusinessDateJobConfig.class,
    StateMachineConfig.class,
    StateMachineActionsConfig.class
})
@TestPropertySource(
    value = "classpath:/application.properties",
    properties = "spring.datasource.url=jdbc:h2:mem:db2;MODE=MySQL;DB_CLOSE_ON_EXIT=false;DB_CLOSE_DELAY=-1"
)
public class JobRestartabilityTest {

    private static final long TIMEOUT = 5;

    private static final CurrencyPairEntity CURRENCY_PAIR_EUR_USD = aCurrencyPairEntityBuilder().id(13L).build();
    private static final CurrencyPairEntity CURRENCY_PAIR_USD_JPY = aCurrencyPairEntityBuilder().id(14L).build();

    private static final FxSpotProductEntity FX_SPOT_PRODUCT = aFxSpotProductEntity().id(13L).build();

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2019, 1, 7);

    @MockBean
    private AlertSender alertSender;

    @Autowired
    private DailySettlementPriceRepository dailySettlementPriceRepository;

    @Autowired
    private EODThresholdFutureValueRepository eodThresholdFutureValueRepository;

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private StateMachine<StateMachineConfig.States, StateMachineConfig.Events> stateMachine;

    @Autowired
    private DataSource ds;

    @Value("classpath:common/tradingHours.xml")
    private Resource tradingHours;

    private DailySettlementPriceEntity dailySettlementPriceEntity(CurrencyPairEntity currencyPair, Double rate) {
        return DailySettlementPriceEntity.builder()
            .currencyPair(currencyPair)
            .businessDate(BUSINESS_DATE)
            .dailySettlementPrice(BigDecimal.valueOf(rate))
            .remarks("TEST")
            .build();
    }

    private EODThresholdFutureValueEntity eodthresholdfuturevalue() {
        return EODThresholdFutureValueEntity
            .builder()
            .fxSpotProduct(FX_SPOT_PRODUCT)
            .applicableDate(BUSINESS_DATE)
            .businessDate(BUSINESS_DATE)
            .value(1L)
            .build();
    }

    @Test
    @DatabaseSetup({
        "/common/currency.xml",
        "/common/fx_spot_product.xml",
        "/common/participants.xml",
        "/common/marginAlertConfiguration.xml",
        "/jobRestartabilityTest/initData.xml"
    })
    void shouldExecuteEodInSeveralAttempts() throws Exception {

        List<Pair<String, BatchStatus>> expectedSoFar = new ArrayList<>();

        sendAndWaitForAlerts(EOD, EOD_1_COMPLETED, TIMEOUT);

        expectedSoFar.add(Pair.of(MTM_TRADES_STEP_NAME, FAILED));
        assertThat(getStepExecutonStatuses()).containsExactlyInAnyOrder(expectedSoFar.toArray(new Pair[expectedSoFar.size()]));

        dailySettlementPriceRepository.save(dailySettlementPriceEntity(CURRENCY_PAIR_EUR_USD, 1.13));

        sendAndWaitForAlerts(EOD, EOD_1_COMPLETED, TIMEOUT);

        expectedSoFar.addAll(
            List.of(
                Pair.of(MTM_TRADES_STEP_NAME, COMPLETED),
                Pair.of(NET_TRADES_STEP_NAME, COMPLETED),
                Pair.of(REBALANCE_POSITIONS_STEP_NAME, FAILED)
            )
        );
        assertThat(getStepExecutonStatuses()).containsExactlyInAnyOrder(expectedSoFar.toArray(new Pair[expectedSoFar.size()]));

        eodThresholdFutureValueRepository.save(eodthresholdfuturevalue());

        sendAndWaitForAlerts(EOD, EOD_2_COMPLETED, TIMEOUT);

        expectedSoFar.addAll(
            List.of(
                Pair.of(REBALANCE_POSITIONS_STEP_NAME, COMPLETED),
                Pair.of(ROLL_POSITIONS_STEP_NAME, COMPLETED),
                Pair.of(SWAP_PNL_STEP_NAME, COMPLETED),
                Pair.of(TOTAL_VM_STEP_NAME, COMPLETED),
                Pair.of(MARGIN_COLLATERAL_EXCESS_OR_DEFICIENCY, FAILED)
            )
        );
        assertThat(getStepExecutonStatuses()).containsExactlyInAnyOrder(expectedSoFar.toArray(new Pair[expectedSoFar.size()]));

        dailySettlementPriceRepository.save(dailySettlementPriceEntity(CURRENCY_PAIR_USD_JPY, 99.1));

        sendAndWaitForAlerts(EOD, EOD_2_COMPLETED, TIMEOUT);
        expectedSoFar.addAll(
            List.of(
                Pair.of(MARGIN_COLLATERAL_EXCESS_OR_DEFICIENCY, COMPLETED),
                Pair.of(DAILY_MARKET_DATA_LEDGER_STEP_NAME, COMPLETED),
                Pair.of(LEDGER_CLEANUP_STEP_NAME, COMPLETED),
                Pair.of(SOD_TRANSACTION_DIARY_LEDGER_STEP_NAME, FAILED),
                Pair.of(TRANSACTION_DIARY_RECORD_DATE_SET_STEP_NAME, COMPLETED)
            )
        );
        assertThat(getStepExecutonStatuses()).containsExactlyInAnyOrder(expectedSoFar.toArray(new Pair[expectedSoFar.size()]));

        FlatXmlDataSet dataSet = new FlatXmlDataSet(new FlatXmlProducer(new InputSource(tradingHours.getInputStream())));
        DatabaseOperation.INSERT.execute(new DatabaseDataSourceConnection(ds), dataSet);

        sendAndWaitForAlerts(EOD, EOD_2_COMPLETED, TIMEOUT);
        expectedSoFar.addAll(
            List.of(
                Pair.of(LEDGER_CLEANUP_STEP_NAME, COMPLETED),
                Pair.of(SOD_TRANSACTION_DIARY_LEDGER_STEP_NAME, COMPLETED),
                Pair.of(TRADE_TRANSACTION_DIARY_LEDGER_STEP_NAME, COMPLETED),
                Pair.of(NET_TRANSACTION_DIARY_LEDGER_STEP_NAME, COMPLETED),
                Pair.of(OPEN_POSITIONS_LEDGER_STEP_NAME, COMPLETED),
                Pair.of(COLLATERAL_BALANCE_LEDGER_STEP_NAME, COMPLETED),
                Pair.of(DAILY_MARKET_DATA_LEDGER_STEP_NAME, COMPLETED),
                Pair.of(COLLATERAL_LIST_LEDGER_STEP_NAME, COMPLETED)
            )
        );

        Set<String> ignored = Set.of(ROLL_BUSINESS_DATE_STEP_NAME, EOD_COMPLETE_NOTIFY_STEP_NAME);
        assertThat(
            getStepExecutonStatuses()
                .stream()
                .filter(not(pair -> ignored.contains(pair.getLeft())))
        ).containsExactlyInAnyOrder(expectedSoFar.toArray(new Pair[expectedSoFar.size()]));

    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
    private void sendAndWaitForAlerts(StateMachineConfig.Events event, AlertEventName expectedAlert, long timeout) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(
            invocation -> {
                NewAlert alert = (NewAlert) invocation.getArguments()[0];
                if (expectedAlert == alert.getEventName()) {
                    latch.countDown();
                }
                return null;
            }
        ).when(alertSender).sendAlert(any());

        stateMachine.sendEvent(event);

        latch.await(timeout, SECONDS);

    }

    private List<Pair<String, BatchStatus>> getStepExecutonStatuses() {
        return Stream.of(EOD1_BATCH_JOB_NAME, EOD2_BATCH_JOB_NAME, ROLL_BUSINESS_DATE_JOB_NAME)
            .map(job -> jobExplorer.getJobInstances(job, 0, 100))
            .flatMap(Collection::stream)
            .map(jobExplorer::getJobExecutions)
            .flatMap(Collection::stream)
            .map(JobExecution::getStepExecutions)
            .flatMap(Collection::stream)
            .map(
                execution -> Pair.of(execution.getStepName(), execution.getStatus())
            ).collect(Collectors.toList());
    }
}
