package com.ihsmarkit.tfx.eod.batch;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_FMT;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.eod.model.ParticipantPositionForPair;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceProvider;
import com.ihsmarkit.tfx.eod.service.NetCalculator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@JobScope
public class NettingTasklet implements Tasklet {

    @Autowired
    private final TradeRepository tradeRepository;

    @Autowired
    private final ParticipantPositionRepository participantPositionRepository;

    @Autowired
    private final DailySettlementPriceProvider dailySettlementPriceProvider;

    @Autowired
    private NetCalculator netCalculator;

    @Value("#{jobParameters['businessDate']}")
    private String businessDateStr;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) throws Exception {
        final LocalDate businessDate = LocalDate.parse(businessDateStr, BUSINESS_DATE_FMT);

        final Stream<TradeEntity> novatedTrades = tradeRepository.findAllNovatedForTradeDate(businessDate);
        final Stream<ParticipantPositionEntity> netted = netCalculator.netAllTtrades(novatedTrades)
                .map(t -> mapToParticipantPosition(t, businessDate));

        participantPositionRepository.saveAll(netted::iterator);

        return RepeatStatus.FINISHED;
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private ParticipantPositionEntity mapToParticipantPosition(final ParticipantPositionForPair trade, final LocalDate businessDate) {
        return ParticipantPositionEntity.builder()
                .participant(trade.getParticipant())
                .participantType(trade.getParticipant().getType())
                .currencyPair(trade.getCurrencyPair())
                .amount(AmountEntity.of(trade.getAmount(), trade.getCurrencyPair().getBaseCurrency()))
                .price(dailySettlementPriceProvider.getDailySettlementPrices(businessDate).get(trade.getCurrencyPair()))
                .tradeDate(businessDate)
                .type(ParticipantPositionType.NET)
                .valueDate(businessDate.plusDays(3))
                .build();
    }

}
