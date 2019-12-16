package com.ihsmarkit.tfx.eod.config;

import javax.sql.DataSource;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.Import;
import org.springframework.jmx.support.RegistrationPolicy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.ihsmarkit.tfx.core.dl.config.CoreDlAutoConfiguration;
import com.ihsmarkit.tfx.test.utils.db.DbUnitTestListeners;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ExtendWith(SpringExtension.class)
@ImportAutoConfiguration(classes = CoreDlAutoConfiguration.class)
@EnableMBeanExport(registration = RegistrationPolicy.REPLACE_EXISTING)
@Import({SpringBatchConfig.class, DateConfig.class})
@SpringBatchTest
@ComponentScan(basePackages = { "com.ihsmarkit.tfx.eod.batch", "com.ihsmarkit.tfx.eod.service", "com.ihsmarkit.tfx.eod.mapper"})
@TestPropertySource("classpath:/application.properties")
@DbUnitTestListeners
@DatabaseTearDown("/common/tearDown.xml")
@SuppressWarnings("VisibilityModifier")
public abstract class AbstractSpringBatchTest  {

    @Autowired
    protected JobLauncherTestUtils jobLauncherTestUtils;

    @TestConfiguration
    static class TestConfig {
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
}
