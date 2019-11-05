package com.ihsmarkit.tfx.eod.batch;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.Import;
import org.springframework.jmx.support.RegistrationPolicy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ihsmarkit.tfx.core.dl.config.CoreDlAutoConfiguration;
import com.ihsmarkit.tfx.eod.config.SpringBatchConfig;

@ExtendWith(SpringExtension.class)
@ImportAutoConfiguration(classes = CoreDlAutoConfiguration.class)
@EnableMBeanExport(registration = RegistrationPolicy.REPLACE_EXISTING)
@Import(SpringBatchConfig.class)
@SpringBatchTest
@SuppressWarnings("VisibilityModifier")
public abstract class AbstractSpringBatchTest {

    @Autowired
    protected JobLauncherTestUtils jobLauncherTestUtils;
}
