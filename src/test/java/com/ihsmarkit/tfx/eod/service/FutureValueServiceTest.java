package com.ihsmarkit.tfx.eod.service;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.ihsmarkit.tfx.core.dl.repository.RepositoryTest;

@ContextConfiguration(classes = FutureValueService.class)
class FutureValueServiceTest extends RepositoryTest {

    @Autowired
    private FutureValueService futureValueService;

    @Test
    @Commit
    @DatabaseSetup("/FutureValueServiceTest/unroll_setup.xml")
    @ExpectedDatabase(value = "/FutureValueServiceTest/unroll_expected.xml", assertionMode = NON_STRICT)
    void shouldUnrollFutureValue() {
        futureValueService.unrollFutureValues(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 1));
    }

}