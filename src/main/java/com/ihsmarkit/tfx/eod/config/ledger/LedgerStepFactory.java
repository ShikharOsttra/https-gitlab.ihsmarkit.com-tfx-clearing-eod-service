package com.ihsmarkit.tfx.eod.config.ledger;

import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.database.orm.JpaQueryProvider;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.core.io.Resource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.eod.batch.ledger.RecordDateSetter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@SuppressFBWarnings("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
@Component
@RequiredArgsConstructor
public class LedgerStepFactory {

    private final DataSource dataSource;
    private final EntityManagerFactory entityManagerFactory;
    private final RecordDateSetter recordDateSetter;
    private final StepBuilderFactory steps;

    <I> JpaPagingItemReaderBuilder<I> listReaderBuilder(final JpaQueryProvider jpaQueryProvider, final int pageSize) {
        return new JpaPagingItemReaderBuilder<I>()
            .entityManagerFactory(entityManagerFactory)
            .saveState(false)
            .transacted(false)
            .pageSize(pageSize)
            .queryProvider(jpaQueryProvider);
    }

    <I> JpaPagingItemReader<I> listReader(final JpaQueryProvider jpaQueryProvider, final int pageSize) {
        return this.<I>listReaderBuilder(jpaQueryProvider, pageSize).build();
    }

    <I, O> SimpleStepBuilder<I, O> stepBuilder(final String stepName, final int chunkSize) {
        return steps.get(stepName)
            .listener(recordDateSetter)
            .chunk(chunkSize);
    }

    <I, O> SimpleStepBuilder<I, O> stepBuilder(final String stepName, final int chunkSize, final StepExecutionListener... listeners) {
        final SimpleStepBuilder<I, O> stepBuilder = stepBuilder(stepName, chunkSize);
        Arrays.stream(listeners)
            .forEach(stepBuilder::listener);
        return stepBuilder;
    }

    @SneakyThrows
    <O> ItemWriter<O> listWriter(final Resource sql) {
        return new JdbcBatchItemWriterBuilder<O>()
            .beanMapped()
            .sql(IOUtils.toString(sql.getInputStream()))
            .dataSource(dataSource)
            .build();
    }

    TaskExecutor taskExecutor(final int concurrencyLimit) {
        final SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
        taskExecutor.setConcurrencyLimit(concurrencyLimit);
        return taskExecutor;
    }

    static <I, O> ItemProcessor<I, O> compositeProcessor(final ItemProcessor<?, ?>... processors) {
        final CompositeItemProcessor<I, O> processor = new CompositeItemProcessor<>();
        processor.setDelegates(List.of(processors));
        return processor;
    }

}
