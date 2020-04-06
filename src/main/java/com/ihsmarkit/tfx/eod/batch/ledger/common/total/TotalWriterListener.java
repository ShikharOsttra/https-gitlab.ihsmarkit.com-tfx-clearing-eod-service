package com.ihsmarkit.tfx.eod.batch.ledger.common.total;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.item.ItemWriter;
import org.springframework.core.Ordered;

import lombok.extern.slf4j.Slf4j;


@Slf4j
//todo: persist status to make it restartable
public class TotalWriterListener<O> extends StepExecutionListenerSupport implements Ordered {

    private final List<Supplier<List<O>>> suppliers;
    private final ItemWriter<O> itemWriter;

    public <I> TotalWriterListener(
        final Supplier<I> totalSupplier, final Function<I, List<O>> totalMapper,
        final ItemWriter<O> itemWriter
    ) {
        this(List.of(() -> totalMapper.apply(totalSupplier.get())), itemWriter);
    }

    public <I1, I2> TotalWriterListener(
        final Supplier<I1> totalSupplier1, final Function<I1, List<O>> totalMapper1,
        final Supplier<I2> totalSupplier2, final Function<I2, List<O>> totalMapper2,
        final ItemWriter<O> itemWriter
    ) {
        this(List.of(() -> totalMapper1.apply(totalSupplier1.get()), () -> totalMapper2.apply(totalSupplier2.get())), itemWriter);
    }

    private TotalWriterListener(final List<Supplier<List<O>>> suppliers, final ItemWriter<O> itemWriter) {
        this.suppliers = suppliers;
        this.itemWriter = itemWriter;
    }

    @Override
    public ExitStatus afterStep(final StepExecution stepExecution) {
        if (stepExecution.getStatus() == BatchStatus.COMPLETED) {
            return writeTotals();
        } else {
            return null;
        }
    }

    @Nullable
    private ExitStatus writeTotals() {
        try {
            final List<O> totals = suppliers.stream()
                .flatMap(supplier -> supplier.get().stream())
                .collect(Collectors.toList());
            itemWriter.write(totals);
            return null;
        } catch (final Exception ex) {
            log.error("Error while writing total: ", ex);
            return ExitStatus.FAILED;
        }
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }
}
