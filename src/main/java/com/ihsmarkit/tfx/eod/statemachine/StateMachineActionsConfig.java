package com.ihsmarkit.tfx.eod.statemachine;

import static com.ihsmarkit.tfx.core.dl.entity.eod.EodStage.EOD1_COMPLETE;
import static com.ihsmarkit.tfx.core.domain.type.SystemParameters.BUSINESS_DATE;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_FMT;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_JOB_PARAM_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.CURRENT_TSP_JOB_PARAM_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD1_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD2_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.ROLL_BUSINESS_DATE_JOB_NAME;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.guard.Guard;

import com.ihsmarkit.tfx.core.dl.entity.eod.EodStage;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodStatusCompositeId;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodStatusEntity;
import com.ihsmarkit.tfx.core.dl.repository.SystemParameterRepository;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodStatusRepository;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.SneakyThrows;

@Configuration
@SuppressFBWarnings({
    "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR",
    "PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS",
    "SIC_INNER_SHOULD_BE_STATIC_ANON",
    "UPM_UNCALLED_PRIVATE_METHOD"
})
public class StateMachineActionsConfig {


    @Autowired
    private EodStatusRepository eodStatusRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier(EOD1_BATCH_JOB_NAME)
    private Job eod1Job;

    @Autowired
    @Qualifier(EOD2_BATCH_JOB_NAME)
    private Job eod2Job;

    @Autowired
    @Qualifier(ROLL_BUSINESS_DATE_JOB_NAME)
    private Job rollBusinessDateJob;

    @Autowired
    private SystemParameterRepository systemParameterRepository;

    @Bean
    public Action<StateMachineConfig.States, StateMachineConfig.Events> initAction() {
        return new Action<>() {

            @Override
            public void execute(final StateContext<StateMachineConfig.States, StateMachineConfig.Events> context) {
                final LocalDate businessDate = systemParameterRepository.getParameterValueFailFast(BUSINESS_DATE);
                context.getExtendedState().getVariables().put(BUSINESS_DATE, businessDate);
            }
        };
    }

    @Bean
    public Action<StateMachineConfig.States, StateMachineConfig.Events> eod1runAction() {
        return new Action<>() {
            @Override
            @SneakyThrows
            public void execute(final StateContext<StateMachineConfig.States, StateMachineConfig.Events> context) {
                final LocalDate businessDate = systemParameterRepository.getParameterValueFailFast(BUSINESS_DATE);
                jobLauncher.run(eod1Job, getJobParameters(businessDate));
            }
        };
    }

    @Bean
    public Action<StateMachineConfig.States, StateMachineConfig.Events> eod1CompleteAction() {
        return new Action<>() {
            @Override
            public void execute(final StateContext<StateMachineConfig.States, StateMachineConfig.Events> context) {
                final LocalDate businessDate = systemParameterRepository.getParameterValueFailFast(BUSINESS_DATE);

                eodStatusRepository.save(
                    EodStatusEntity.builder()
                        .id(new EodStatusCompositeId(EOD1_COMPLETE, businessDate))
                        .timestamp(LocalDateTime.now())
                        .build()
                );
            }
        };
    }

    @Bean
    public Action<StateMachineConfig.States, StateMachineConfig.Events> eod2runAction() {
        return new Action<>() {
            @Override
            @SneakyThrows
            public void execute(final StateContext<StateMachineConfig.States, StateMachineConfig.Events> context) {
                final LocalDate businessDate = systemParameterRepository.getParameterValueFailFast(BUSINESS_DATE);
                jobLauncher.run(eod2Job, getJobParameters(businessDate));
            }
        };
    }

    @Bean
    public Action<StateMachineConfig.States, StateMachineConfig.Events> ledgerRunAction() {
        return new Action<>() {
            @Override
            public void execute(final StateContext<StateMachineConfig.States, StateMachineConfig.Events> context) {
                //context.getStateMachine().setStateMachineError(new RuntimeException("dddddddddddd"));
            }
        };
    }

    @Bean
    public Action<StateMachineConfig.States, StateMachineConfig.Events> dateRollRunAction() {
        return new Action<>() {
            @Override
            @SneakyThrows
            public void execute(final StateContext<StateMachineConfig.States, StateMachineConfig.Events> context) {
                final LocalDate businessDate = systemParameterRepository.getParameterValueFailFast(BUSINESS_DATE);
                jobLauncher.run(rollBusinessDateJob, getJobParameters(businessDate));            }
        };
    }

    @Bean
    public Guard<StateMachineConfig.States, StateMachineConfig.Events> swpPointApprovedGuard() {
        return new Guard<StateMachineConfig.States, StateMachineConfig.Events>() {

            @Override
            public boolean evaluate(final StateContext<StateMachineConfig.States, StateMachineConfig.Events> context) {
                final LocalDate date = (LocalDate) context.getExtendedState().getVariables().get(BUSINESS_DATE);
                return eodStatusRepository.existsById(new EodStatusCompositeId(EodStage.SWAP_POINTS_APPROVED, date));
            }
        };
    }

    @Bean
    public Guard<StateMachineConfig.States, StateMachineConfig.Events> dspApprovedGuard() {
        return new Guard<StateMachineConfig.States, StateMachineConfig.Events>() {

            @Override
            public boolean evaluate(final StateContext<StateMachineConfig.States, StateMachineConfig.Events> context) {
                final LocalDate date = (LocalDate) context.getExtendedState().getVariables().get(BUSINESS_DATE);
                return eodStatusRepository.existsById(new EodStatusCompositeId(EodStage.DSP_APPROVED, date));
            }
        };
    }

    @Bean
    public Guard<StateMachineConfig.States, StateMachineConfig.Events> tradesInFlightGuard() {
        return new Guard<StateMachineConfig.States, StateMachineConfig.Events>() {
            @Override
            public boolean evaluate(final StateContext<StateMachineConfig.States, StateMachineConfig.Events> context) {
                final LocalDate date = (LocalDate) context.getExtendedState().getVariables().get(BUSINESS_DATE);
                final boolean tradesInFlight = tradeRepository.existsTradeInFlightForDate(date);
                return !tradesInFlight;
            }
        };
    }

    @SneakyThrows
    private JobParameters getJobParameters(final LocalDate businessDate) {
        return new JobParametersBuilder()
            .addString(BUSINESS_DATE_JOB_PARAM_NAME, businessDate.format(BUSINESS_DATE_FMT))
            .addString(CURRENT_TSP_JOB_PARAM_NAME, LocalDateTime.now().toString())
            .toJobParameters();
    }
}
