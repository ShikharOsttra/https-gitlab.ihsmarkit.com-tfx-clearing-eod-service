package com.ihsmarkit.tfx.eod.integration;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.Import;
import org.springframework.jmx.support.RegistrationPolicy;

import com.ihsmarkit.tfx.core.dl.config.CoreDlAutoConfiguration;
import com.ihsmarkit.tfx.eod.config.CacheConfig;
import com.ihsmarkit.tfx.eod.config.DateConfig;
import com.ihsmarkit.tfx.eod.config.SpringBatchConfig;

@Configuration
@ImportAutoConfiguration(classes = CoreDlAutoConfiguration.class)
@Import({DateConfig.class, CacheConfig.class, SpringBatchConfig.class})
@EnableMBeanExport(registration = RegistrationPolicy.REPLACE_EXISTING)
@ComponentScan(basePackages = { "com.ihsmarkit.tfx.eod.batch", "com.ihsmarkit.tfx.eod.service", "com.ihsmarkit.tfx.eod.mapper"})
class IntegrationTestConfig {
}
