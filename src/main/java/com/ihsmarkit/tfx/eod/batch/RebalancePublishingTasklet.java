package com.ihsmarkit.tfx.eod.batch;

import java.time.LocalDate;
import java.util.List;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.eod.service.PositionRebalancePublishingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@JobScope
@Slf4j
public class RebalancePublishingTasklet implements Tasklet {

    private static final String TASKLET_LABEL = "[rebalancePublishingPositions]";

    private final TradeRepository tradeRepository;

    private final PositionRebalancePublishingService publishingService;

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) {
        log.info("{} start", TASKLET_LABEL);
        log.info("{} loading rebalanced trades", TASKLET_LABEL);
        final List<TradeEntity> trades = tradeRepository.findAllBalanceByTradeDate(businessDate);

        log.info("{} publishing rebalance results for {} trades", TASKLET_LABEL, trades.size());
        publishingService.publishTrades(businessDate, trades);

        log.info("{} end", TASKLET_LABEL);
        return RepeatStatus.FINISHED;
    }
}
