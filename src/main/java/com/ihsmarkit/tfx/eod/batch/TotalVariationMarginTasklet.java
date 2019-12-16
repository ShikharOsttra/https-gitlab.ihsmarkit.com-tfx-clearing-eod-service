package com.ihsmarkit.tfx.eod.batch;

import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.DAILY_MTM;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.INITIAL_MTM;
import static com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType.SWAP_PNL;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodProductCashSettlementRepository;
import com.ihsmarkit.tfx.eod.mapper.ParticipantCurrencyPairAmountMapper;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.EodCashSettlementMappingService;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@JobScope
public class TotalVariationMarginTasklet implements Tasklet {

    private final EodProductCashSettlementRepository eodProductCashSettlementRepository;

    private final EODCalculator eodCalculator;

    private final EodCashSettlementMappingService eodCashSettlementMappingService;

    private final ParticipantCurrencyPairAmountMapper mapper;

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) {

        final Stream<EodProductCashSettlementEntity> margin =
            eodProductCashSettlementRepository.findByDateAndTypeIn(businessDate, DAILY_MTM, INITIAL_MTM, SWAP_PNL);

        final Stream<EodProductCashSettlementEntity> totalVM =
            eodCalculator.netAll(margin.map(mapper::toParticipantCurrencyPairAmount))
                .map(eodCashSettlementMappingService::mapTotalVM);

        eodProductCashSettlementRepository.saveAll(totalVM::iterator);

        return RepeatStatus.FINISHED;
    }

}
