package com.ihsmarkit.tfx.eod.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamWriter;

import lombok.SneakyThrows;

@ExtendWith(MockitoExtension.class)
class ListItemWriterTest {

    @Mock
    private ItemStreamWriter<String> delegate;

    @InjectMocks
    private ListItemWriter<String> writer;

    @Test
    @SneakyThrows
    void shouldDelegateWrite() {
        final var item1 = "1";
        final var item2 = "2";

        writer.write(List.of(List.of(item1), List.of(item2)));

        verify(delegate).write(argThat(items -> {
            assertThat(items).isEqualTo(List.of(item1, item2));
            return true;
        }));
    }

    @Test
    void shouldDelegateOpen() {
        final var executionContext = mock(ExecutionContext.class);
        writer.open(executionContext);
        verify(delegate).open(executionContext);
    }

    @Test
    void shouldDelegateUpdate() {
        final var executionContext = mock(ExecutionContext.class);
        writer.update(executionContext);
        verify(delegate).update(executionContext);
    }

    @Test
    void shouldDelegateClose() {
        writer.close();
        verify(delegate).close();
    }
}