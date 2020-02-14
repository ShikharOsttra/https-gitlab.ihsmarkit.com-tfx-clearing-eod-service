package com.ihsmarkit.tfx.eod.integration;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.Import;
import org.springframework.jmx.support.RegistrationPolicy;

import com.ihsmarkit.tfx.alert.client.jms.AlertSender;
import com.ihsmarkit.tfx.collateral.calculator.config.CollateralCalculatorConfiguration;
import com.ihsmarkit.tfx.core.dl.config.CoreDlAutoConfiguration;
import com.ihsmarkit.tfx.core.time.ClockService;
import com.ihsmarkit.tfx.eod.config.CacheConfig;
import com.ihsmarkit.tfx.eod.config.DateConfig;
import com.ihsmarkit.tfx.eod.config.SpringBatchConfig;
import com.ihsmarkit.tfx.eod.config.listeners.EodAlertStepListener;
import com.ihsmarkit.tfx.eod.config.listeners.EodJobListenerFactory;
import com.ihsmarkit.tfx.mailing.config.MailingAutoConfiguration;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings({ "FCBL_FIELD_COULD_BE_LOCAL", "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR" })
@Configuration
@ImportAutoConfiguration(classes = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class,
    BatchAutoConfiguration.class,
    MailingAutoConfiguration.class,
    CollateralCalculatorConfiguration.class,

    CoreDlAutoConfiguration.class
})
@Import({
    DateConfig.class,
    CacheConfig.class,
    SpringBatchConfig.class,
    EodJobListenerFactory.class,
    EodAlertStepListener.class,

    ClockService.class
})
@EnableMBeanExport(registration = RegistrationPolicy.REPLACE_EXISTING)
@ComponentScan(basePackages = { "com.ihsmarkit.tfx.eod.batch", "com.ihsmarkit.tfx.eod.service", "com.ihsmarkit.tfx.eod.mapper"})
class IntegrationTestConfig {

    @MockBean
    private AlertSender alertSender;
}
