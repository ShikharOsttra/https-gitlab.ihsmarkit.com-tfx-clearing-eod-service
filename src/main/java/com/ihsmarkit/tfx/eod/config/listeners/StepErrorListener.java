package com.ihsmarkit.tfx.eod.config.listeners;

import java.util.List;

import javax.annotation.Nullable;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.util.CollectionUtils;

public abstract class StepErrorListener extends StepExecutionListenerSupport {

    @Override
    public final ExitStatus afterStep(final StepExecution stepExecution) {
        final List<Throwable> exceptions = stepExecution.getFailureExceptions();

        if (!CollectionUtils.isEmpty(exceptions)) {
            onExceptionsAfterStep(stepExecution, exceptions);
        }

        return null;
    }

    protected abstract void onExceptionsAfterStep(StepExecution stepExecution, List<Throwable> stepExceptions);

    @Nullable
    static Throwable findCauseByType(final List<Throwable> stepExceptions, final Class<? extends Throwable> wantedException) {
        return stepExceptions.stream()
            .filter(wantedException::isInstance)
            .findFirst()
            .map(Throwable::getCause)
            .orElse(null);
    }
}
