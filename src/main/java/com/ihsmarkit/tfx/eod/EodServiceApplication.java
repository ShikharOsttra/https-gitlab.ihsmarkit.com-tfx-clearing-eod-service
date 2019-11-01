package com.ihsmarkit.tfx.eod;

import com.ihsmarkit.tfx.eod.batch.MarkToMarketTradesTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
@EnableScheduling
@SpringBootApplication
public class EodServiceApplication {
    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private JobBuilderFactory jobs;

    @Autowired
    private StepBuilderFactory steps;

    @Bean
    public Step eod1MtmStep(MarkToMarketTradesTasklet mtmTasklet){
        return steps.get("eod1MtmStep")
                .tasklet(mtmTasklet)
                .build();
    }

    @Bean
    public Job eod1Job(Step eod1MtmStep) {
        return jobs.get("eod1Job")
                .flow(eod1MtmStep)
                .end()
                .build();
    }

    public static void main(final String[] args) {
        SpringApplication.run(EodServiceApplication.class, args);
    }
}
