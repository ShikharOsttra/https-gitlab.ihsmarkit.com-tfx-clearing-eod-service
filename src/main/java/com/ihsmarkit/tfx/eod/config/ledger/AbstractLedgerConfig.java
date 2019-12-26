package com.ihsmarkit.tfx.eod.config.ledger;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.database.orm.JpaQueryProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

import com.ihsmarkit.tfx.eod.batch.ledger.RecordDateSetter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.SneakyThrows;

@SuppressFBWarnings("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
class AbstractLedgerConfig<I, O> {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private RecordDateSetter recordDateSetter;

    @Autowired
    private StepBuilderFactory steps;

    protected JpaPagingItemReaderBuilder<I> listReader(final JpaQueryProvider jpaQueryProvider) {
        return new JpaPagingItemReaderBuilder<I>()
            .entityManagerFactory(entityManagerFactory)
            .saveState(false)
            .transacted(false)
            .queryProvider(jpaQueryProvider);
    }

    protected SimpleStepBuilder<I, O> stepBuilder(final String stepName, final int chunkSize) {
        return steps.get(stepName)
            .listener(recordDateSetter)
            .chunk(chunkSize);
    }

    @SneakyThrows
    protected ItemWriter<O> listWriter(final Resource sql) {
        return new JdbcBatchItemWriterBuilder<O>()
            .beanMapped()
            .sql(IOUtils.toString(sql.getInputStream()))
            .dataSource(dataSource)
            .build();
    }
}
