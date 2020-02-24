package com.ihsmarkit.tfx.eod.support;

import java.util.function.Consumer;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BeforeStepListener extends StepExecutionListenerSupport {

    private final Consumer<StepExecution> beforeStepConsumer;

    @Override
    public void beforeStep(final StepExecution stepExecution) {
        beforeStepConsumer.accept(stepExecution);
    }
}
