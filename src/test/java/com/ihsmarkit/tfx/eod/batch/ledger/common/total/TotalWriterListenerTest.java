package com.ihsmarkit.tfx.eod.batch.ledger.common.total;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.batch.core.ExitStatus.FAILED;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ItemWriter;

import lombok.SneakyThrows;

@ExtendWith(MockitoExtension.class)
class TotalWriterListenerTest {

    @Mock
    private ItemWriter itemWriter;

    private TotalWriterListener totalWriterListener;

    @BeforeEach
    void setUp() {
        totalWriterListener = new TotalWriterListener(Object::new, List::of, itemWriter);
    }

    @Test
    void shouldNotWriteTotalWhenStepFailed() {
        final StepExecution stepExecution = mock(StepExecution.class);
        when(stepExecution.getStatus()).thenReturn(BatchStatus.FAILED);

        assertThat(totalWriterListener.afterStep(stepExecution)).isNull();

        verifyZeroInteractions(itemWriter);
    }

    @Test
    @SneakyThrows
    void shouldFailStepWhenThrowException() {
        final StepExecution stepExecution = mock(StepExecution.class);
        when(stepExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);

        doThrow(new RuntimeException()).when(itemWriter).write(any());

        assertThat(totalWriterListener.afterStep(stepExecution)).isEqualTo(FAILED);
    }

}