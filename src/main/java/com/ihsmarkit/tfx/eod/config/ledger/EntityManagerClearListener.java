package com.ihsmarkit.tfx.eod.config.ledger;

import javax.persistence.EntityManager;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class EntityManagerClearListener implements StepExecutionListener {

    private final EntityManager entityManager;

    @Override
    public void beforeStep(final StepExecution stepExecution) {
    }

    @Override
    public ExitStatus afterStep(final StepExecution stepExecution) {
        //need to clear session to remove previously loaded trades and related associations to avoid expensive "Dirty checking collections" on every flush
        entityManager.clear();
        return stepExecution.getExitStatus();
    }
}
