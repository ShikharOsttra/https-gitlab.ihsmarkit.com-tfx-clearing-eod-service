package com.ihsmarkit.tfx.eod.batch.ledger;

import java.time.LocalDate;

import org.springframework.data.util.Lazy;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.repository.SystemParameterRepository;
import com.ihsmarkit.tfx.core.domain.type.SystemParameters;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EvaluationDateProvider {

    private final SystemParameterRepository systemParameterRepository;

    private final Lazy<LocalDate> evaluationDate = Lazy.of(this::loadEvaluationDate);

    public LocalDate get() {
        return evaluationDate.get();
    }

    private LocalDate loadEvaluationDate() {
        return systemParameterRepository.getParameterValueFailFast(SystemParameters.EVALUATION_DATE);
    }

}
