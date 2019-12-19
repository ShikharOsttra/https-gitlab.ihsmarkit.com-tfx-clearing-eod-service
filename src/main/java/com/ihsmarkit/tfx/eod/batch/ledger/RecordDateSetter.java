package com.ihsmarkit.tfx.eod.batch.ledger;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.time.ClockService;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class RecordDateSetter extends StepExecutionListenerSupport {

    private static final String RECORD_DATE = "recordDate";

    private final ClockService clockService;

    @Override
    public void beforeStep(final StepExecution stepExecution) {
        //todo: configure execution context serializer with proper object mapper
        stepExecution.getExecutionContext().put(RECORD_DATE, clockService.getCurrentDateTime().toString());
    }
}
