package com.ihsmarkit.tfx.eod.batch;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.job.DefaultJobParametersValidator;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class Eod1JobParametersValidator extends DefaultJobParametersValidator {

//    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public void validate(final JobParameters parameters) throws JobParametersInvalidException {
//        final LocalDate closingDate = LocalDate.parse(parameters.getString("businessDate"), DATE_FORMATTER);
        // todo: validate businessDate
//        if (!valid) {
//            throw new JobParametersInvalidException("Invalid 'businessDate' supplied.");
//        }
    }
}
