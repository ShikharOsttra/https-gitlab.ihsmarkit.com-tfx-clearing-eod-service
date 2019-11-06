package com.ihsmarkit.tfx.eod.config;

import static com.ihsmarkit.tfx.core.time.ClockService.JST;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD1_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.JOB_NAME_PARAM_NAME;

import java.io.IOException;
import java.util.Properties;
import java.util.TimeZone;

import javax.sql.DataSource;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.ScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.spi.JobFactory;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
public class QuartzConfig {

    private static final String EOD1_JOB_TRIGGER1_NAME = "eod1JobTrigger1";
    private static final String EOD1_JOB_TRIGGER2_NAME = "eod1JobTrigger2";
    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone(JST);

    @Value("${eod1.job.trigger1.cron}")
    private final String eod1JobTrigger1Cron;

    @Value("${eod1.job.trigger2.cron}")
    private final String eod1JobTrigger2Cron;

    private final DataSource dataSource;

    @Bean
    public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor(final JobRegistry jobRegistry) {
        final JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor = new JobRegistryBeanPostProcessor();
        jobRegistryBeanPostProcessor.setJobRegistry(jobRegistry);

        return jobRegistryBeanPostProcessor;
    }


    @Bean
    public JobDetail eod1JobDetail() {
        final JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(JOB_NAME_PARAM_NAME, EOD1_BATCH_JOB_NAME);

        return JobBuilder.newJob(EodQuartzJob.class)
            .withIdentity(EOD1_BATCH_JOB_NAME)
            .setJobData(jobDataMap)
            .storeDurably()
            .build();
    }

    @Bean
    public Trigger eod1JobTrigger1() {
        final ScheduleBuilder<CronTrigger> scheduleBuilder = CronScheduleBuilder
            .cronSchedule(eod1JobTrigger1Cron)
            .inTimeZone(TIME_ZONE);

        return TriggerBuilder
            .newTrigger()
            .forJob(eod1JobDetail())
            .withIdentity(EOD1_JOB_TRIGGER1_NAME)
            .withSchedule(scheduleBuilder)
            .build();
    }

    @Bean
    public Trigger eod1JobTrigger2() {
        final ScheduleBuilder<CronTrigger> scheduleBuilder = CronScheduleBuilder
            .cronSchedule(eod1JobTrigger2Cron)
            .inTimeZone(TIME_ZONE);

        return TriggerBuilder
            .newTrigger()
            .forJob(eod1JobDetail())
            .withIdentity(EOD1_JOB_TRIGGER2_NAME)
            .withSchedule(scheduleBuilder)
            .build();
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(final ApplicationContext applicationContext) throws IOException {
        final SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
        final JobFactory jobFactory = new SpringBeanJobFactory();
        ((ApplicationContextAware) jobFactory).setApplicationContext(applicationContext);
        schedulerFactoryBean.setJobFactory(jobFactory);
        schedulerFactoryBean.setTriggers(eod1JobTrigger1(), eod1JobTrigger2());
        schedulerFactoryBean.setDataSource(dataSource);
        schedulerFactoryBean.setQuartzProperties(quartzProperties());
        schedulerFactoryBean.setJobDetails(eod1JobDetail());

        return schedulerFactoryBean;
    }

    @Bean
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public Properties quartzProperties() throws IOException {
        final PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
        propertiesFactoryBean.setLocation(new ClassPathResource("/quartz.properties"));
        propertiesFactoryBean.afterPropertiesSet();

        return propertiesFactoryBean.getObject();
    }
}
