package com.ihsmarkit.tfx.eod.config;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.Import;
import org.springframework.jmx.support.RegistrationPolicy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ihsmarkit.tfx.alert.client.jms.AlertSender;
import com.ihsmarkit.tfx.core.dl.config.CoreDlAutoConfiguration;
import com.ihsmarkit.tfx.eod.config.listeners.EodJobListenerFactory;
import com.ihsmarkit.tfx.eod.config.listeners.EodAlertStepListener;
import com.ihsmarkit.tfx.test.utils.db.DbUnitTestListeners;

@ExtendWith(SpringExtension.class)
@ImportAutoConfiguration(classes = CoreDlAutoConfiguration.class)
@EnableMBeanExport(registration = RegistrationPolicy.REPLACE_EXISTING)
@Import({ SpringBatchConfig.class, DateConfig.class, InMemoryJobRepositoryConfiguration.class, EodJobListenerFactory.class, EodAlertStepListener.class })
@SpringBatchTest
@ComponentScan(basePackages = { "com.ihsmarkit.tfx.eod.batch", "com.ihsmarkit.tfx.eod.service", "com.ihsmarkit.tfx.eod.mapper" })
@TestPropertySource("classpath:/application.properties")
@DbUnitTestListeners
@SuppressWarnings("VisibilityModifier")
public abstract class AbstractSpringBatchTest {

    @Autowired
    protected JobLauncherTestUtils jobLauncherTestUtils;

    @MockBean
    protected AlertSender alertSender;

}
