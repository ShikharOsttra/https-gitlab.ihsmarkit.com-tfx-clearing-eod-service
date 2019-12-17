package com.ihsmarkit.tfx.eod.config;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@TestConfiguration
public class InMemoryJobRepositoryConfiguration {

    @Bean
    @SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
    public BatchConfigurer getBatchConfigurer() {
        return new DefaultBatchConfigurer() {
            @Override
            public void setDataSource(DataSource dataSource) {
            }
        };
    }

}
