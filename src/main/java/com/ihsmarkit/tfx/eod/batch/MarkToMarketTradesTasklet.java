package com.ihsmarkit.tfx.eod.batch;

//import static com.ihsmarkit.tfx.eod.config.SpringBatchConfig.BUSINESS_DATE_FMT;

//import java.time.LocalDate;
//import java.util.Collection;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

//import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
//import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@JobScope
public class MarkToMarketTradesTasklet implements Tasklet {

//    private final TradeRepository tradeRepository;

    @Value("#{jobParameters['businessDate']}")
    private String businessDateStr;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) throws Exception {
//        final LocalDate businessDate = LocalDate.parse(businessDateStr, BUSINESS_DATE_FMT);

        // todo: replace w/ correct method
//        Collection<TradeEntity> novatedTrades = tradeRepository.findAllNovatedTradesForBusinessDate(businessDate);
        // todo: perform MtM

        return RepeatStatus.FINISHED;
    }
}
