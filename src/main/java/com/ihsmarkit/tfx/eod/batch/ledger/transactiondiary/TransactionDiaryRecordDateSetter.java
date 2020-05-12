package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.time.ClockService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class TransactionDiaryRecordDateSetter implements Tasklet {

    private static final String TRANSACTION_DIARY_RECORD_DATE = "transactionDiaryRecordDate";

    private final ClockService clockService;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) throws Exception {
        final ExecutionContext jobExecutionContext = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
        jobExecutionContext.put(TRANSACTION_DIARY_RECORD_DATE, clockService.getCurrentDateTime().toString());
        return RepeatStatus.FINISHED;
    }
}
