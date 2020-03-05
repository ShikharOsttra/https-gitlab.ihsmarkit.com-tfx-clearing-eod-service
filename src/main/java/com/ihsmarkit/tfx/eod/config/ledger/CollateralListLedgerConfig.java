package com.ihsmarkit.tfx.eod.config.ledger;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.COLLATERAL_LIST_LEDGER_STEP_NAME;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;

import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralBalanceEntity;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.CollateralListLedgerProcessor;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.CollateralListQueryProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.CollateralListTotalProcessor;
import com.ihsmarkit.tfx.eod.model.ledger.CollateralListItem;

import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
public class CollateralListLedgerConfig {

    @Value("${eod.ledger.collateral.list.chunk.size:1000}")
    private final int collateralListChunkSize;
    @Value("${eod.ledger.collateral.list.concurrency.limit:1}")
    private final int collateralListConcurrencyLimit;
    @Value("classpath:/ledger/sql/eod_ledger_collateral_list_insert.sql")
    private final Resource collateralListLedgerSql;

    private final CollateralListTotalProcessor collateralListTotalProcessor;
    private final CollateralListLedgerProcessor collateralListProcessor;
    private final LedgerStepFactory ledgerStepFactory;

    @Bean(COLLATERAL_LIST_LEDGER_STEP_NAME)
    Step collateralListLedger() {
        return ledgerStepFactory.<CollateralBalanceEntity, CollateralListItem<String>>stepBuilder(COLLATERAL_LIST_LEDGER_STEP_NAME, collateralListChunkSize)
            .reader(collateralListReader())
            .processor(collateralListItemProcessor())
            .writer(collateralListWriter())
            .taskExecutor(collateralListTaskExecutor())
            .build();
    }

    @Bean
    TaskExecutor collateralListTaskExecutor() {
        return ledgerStepFactory.taskExecutor(collateralListConcurrencyLimit);
    }

    @Bean
    ItemReader<CollateralBalanceEntity> collateralListReader() {
        return ledgerStepFactory.listReader(new CollateralListQueryProvider(), collateralListChunkSize);
    }

    @Bean
    @StepScope
    ItemProcessor<CollateralBalanceEntity, CollateralListItem<String>> collateralListItemProcessor() {
        return collateralListTotalProcessor.wrapItemProcessor(
            collateralListProcessor,
            collateralListItem ->
                CollateralListItem.<String>builder()
                    .businessDate(collateralListItem.getBusinessDate())
                    .tradeDate(collateralListItem.getTradeDate())
                    .evaluationDate(collateralListItem.getEvaluationDate())
                    .recordDate(collateralListItem.getRecordDate())
                    .participantCode(collateralListItem.getParticipantCode())
                    .participantName(collateralListItem.getParticipantName())
                    .participantType(collateralListItem.getParticipantType())
                    .collateralPurposeType(collateralListItem.getCollateralPurposeType())
                    .collateralPurpose(collateralListItem.getCollateralPurpose())
                    .collateralName(collateralListItem.getCollateralName())
                    .collateralType(collateralListItem.getCollateralType())
                    .collateralTypeNo(collateralListItem.getCollateralTypeNo())
                    .securityCode(collateralListItem.getSecurityCode())
                    .isinCode(collateralListItem.getIsinCode())
                    .amount(collateralListItem.getAmount())
                    .marketPrice(collateralListItem.getMarketPrice())
                    .evaluatedPrice(collateralListItem.getEvaluatedPrice())
                    .bojCode(collateralListItem.getBojCode())
                    .jasdecCode(collateralListItem.getJasdecCode())
                    .interestPaymentDay(collateralListItem.getInterestPaymentDay())
                    .interestPaymentDay2(collateralListItem.getInterestPaymentDay2())
                    .maturityDate(collateralListItem.getMaturityDate())
                    .orderId(collateralListItem.getOrderId())
                    .recordType(collateralListItem.getRecordType())

                    .evaluatedAmount(collateralListItem.getEvaluatedAmount().toString())
                    .build()
        );
    }

    @Bean
    @StepScope
    ItemWriter<CollateralListItem<String>> collateralListWriter() {
        return collateralListTotalProcessor.wrapItemWriter(collateralListJdbcWriter());
    }

    @Bean
    ItemWriter<CollateralListItem<String>> collateralListJdbcWriter() {
        return ledgerStepFactory.listWriter(collateralListLedgerSql);
    }
}
