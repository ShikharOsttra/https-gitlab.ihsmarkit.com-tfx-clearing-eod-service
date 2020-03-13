package com.ihsmarkit.tfx.eod.statemachine;

import static com.ihsmarkit.tfx.core.domain.eod.EodStage.DSP_APPROVED;
import static com.ihsmarkit.tfx.core.domain.eod.EodStage.EOD1_COMPLETE;
import static com.ihsmarkit.tfx.core.domain.eod.EodStage.EOD2_COMPLETE;
import static com.ihsmarkit.tfx.core.domain.eod.EodStage.SWAP_POINTS_APPROVED;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.ihsmarkit.tfx.core.dl.entity.eod.EodStatusCompositeId;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodStatusEntity;
import com.ihsmarkit.tfx.core.dl.repository.SystemParameterRepository;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodStatusRepository;
import com.ihsmarkit.tfx.core.domain.eod.EodStage;
import com.ihsmarkit.tfx.core.domain.type.SystemParameter;
import com.ihsmarkit.tfx.core.domain.type.SystemParameters;
import com.ihsmarkit.tfx.core.time.ClockService;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Configuration
@RequiredArgsConstructor
public class StateMachineActionsConfig {

    public static final String BUSINESS_DATE_ATTRIBUTE = "BUSINESS_DATE";

    private final EodStatusRepository eodStatusRepository;

    private final TradeRepository tradeRepository;

    private final JobLauncher jobLauncher;

    @Qualifier(EOD1_BATCH_JOB_NAME)
    private final Job eod1Job;

    @Qualifier(EOD2_BATCH_JOB_NAME)
    private final Job eod2Job;

    @Qualifier(ROLL_BUSINESS_DATE_JOB_NAME)
    private final Job rollBusinessDateJob;

    private final SystemParameterRepository systemParameterRepository;

    private final ClockService clockService;

    @Bean
    public Action<StateMachineConfig.States, StateMachineConfig.Events> initAction() {
        return context -> context.getExtendedState().getVariables()
                    .put(BUSINESS_DATE_ATTRIBUTE, systemParameterRepository.getParameterValueFailFast(BUSINESS_DATE));
    }

    @Bean
    public JobEodAction eod1runAction() {
        return businessDate -> jobLauncher.run(eod1Job, getJobParameters(businessDate));
    }

    @Bean
    public EodAction eod1CompleteAction() {
        return businessDate -> saveEodStatus(EOD1_COMPLETE, businessDate);

    }

    @Bean
    public EodAction eod2CompleteAction() {
        return businessDate -> saveEodStatus(EOD2_COMPLETE, businessDate);
    }

    @Bean
    public JobEodAction eod2runAction() {
        return businessDate -> jobLauncher.run(eod2Job, getJobParameters(businessDate));
    }

    @Bean
    public JobEodAction dateRollRunAction() {
        return businessDate -> jobLauncher.run(rollBusinessDateJob, getJobParameters(businessDate));
    }

    @Bean
    public EodGuard swpPointApprovedGuard() {
        return date -> eodStatusRepository.existsById(new EodStatusCompositeId(SWAP_POINTS_APPROVED, date));
    }

    @Bean
    public EodGuard dspApprovedGuard() {
        return date -> eodStatusRepository.existsById(new EodStatusCompositeId(DSP_APPROVED, date));
    }

    @Bean
    public EodGuard collateralPriceUploadedGuard() {
        return businessDate -> isCollateralPriceUploadCompleted(businessDate, SystemParameters.LAST_EOD_PRICES_BOND_UPDATE_DATE_TIME)
                            && isCollateralPriceUploadCompleted(businessDate, SystemParameters.LAST_EOD_PRICES_EQUITY_UPDATE_DATE_TIME);
    }

    @Bean
    public EodGuard tradesInFlightGuard() {
        return date -> !tradeRepository.existsTradeInFlightForDate(date);
    }

    private void saveEodStatus(final EodStage stage, final LocalDate businessDate) {
        eodStatusRepository.save(
            EodStatusEntity.builder()
                .id(new EodStatusCompositeId(stage, businessDate))
                .timestamp(clockService.getCurrentDateTimeUTC())
                .build()
        );
    }

    @SneakyThrows
    private JobParameters getJobParameters(final LocalDate businessDate) {
        return new JobParametersBuilder()
            .addString(BUSINESS_DATE_JOB_PARAM_NAME, businessDate.format(BUSINESS_DATE_FMT))
            .addString(CURRENT_TSP_JOB_PARAM_NAME, clockService.getCurrentDateTimeUTC().toString())
            .toJobParameters();
    }

    private boolean isCollateralPriceUploadCompleted(final LocalDate businessDate, final SystemParameter<LocalDateTime> collateralUpdateParameter) {
        return systemParameterRepository.getParameterValueFailFast(collateralUpdateParameter).isAfter(businessDate.atStartOfDay());
    }
}
