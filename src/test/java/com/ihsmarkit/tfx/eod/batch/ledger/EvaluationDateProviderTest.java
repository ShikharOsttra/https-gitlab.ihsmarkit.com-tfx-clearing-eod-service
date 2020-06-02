package com.ihsmarkit.tfx.eod.batch.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ihsmarkit.tfx.core.dl.repository.SystemParameterRepository;
import com.ihsmarkit.tfx.core.domain.type.SystemParameters;

@ExtendWith(MockitoExtension.class)
class EvaluationDateProviderTest {

    private static final LocalDate EVALUATION_DATE = LocalDate.of(2020, 1, 5);

    @Mock
    private SystemParameterRepository systemParameterRepository;

    @InjectMocks
    private EvaluationDateProvider evaluationDateProvider;

    @Test
    void shouldProvideEvaluationDate() {
        when(systemParameterRepository.getParameterValueFailFast(SystemParameters.EVALUATION_DATE)).thenReturn(EVALUATION_DATE);

        assertThat(evaluationDateProvider.get()).isEqualTo(EVALUATION_DATE);
    }

}