package com.ihsmarkit.tfx.eod.batch.ledger.common.total;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;

@ExtendWith(MockitoExtension.class)
class AbstractTotalHolderTest {

    private static final String NAME = "name";
    private static final String TOTAL_KEY = NAME + ".total";

    @Mock
    private ExecutionContext executionContext;

    @Test
    void shouldInitValueWhenSaveStateIsTrue() {
        final TestTotalHolder holder = spy(new TestTotalHolder(true));
        final Object savedState = new Object();

        when(executionContext.get(TOTAL_KEY)).thenReturn(savedState);

        holder.open(executionContext);

        verify(holder).initValue(savedState);
    }

    @Test
    void shouldInitValueWithNullWhenSaveStateIsFalse() {
        final TestTotalHolder holder = spy(new TestTotalHolder(false));

        holder.open(executionContext);

        verify(holder).initValue(null);
        verifyZeroInteractions(executionContext);
    }

    @Test
    void shouldUpdateConextWhenSaveStateIsTrue() {
        final TestTotalHolder holder = spy(new TestTotalHolder(true));
        final Object state = new Object();
        when(holder.get()).thenReturn(state);

        holder.update(executionContext);

        verify(executionContext).put(TOTAL_KEY, state);
    }

    @Test
    void shouldNotUpdateContextWhenSaveStateIsFalse() {
        final TestTotalHolder holder = spy(new TestTotalHolder(false));

        holder.update(executionContext);

        verifyZeroInteractions(executionContext);
    }

    private static class TestTotalHolder extends AbstractTotalHolder {

        TestTotalHolder(final boolean saveState) {
            super(NAME, saveState);
        }

        @Override
        protected void initValue(@Nullable final Object value) {

        }

        @Override
        public Object get() {
            return new Object();
        }
    }

}