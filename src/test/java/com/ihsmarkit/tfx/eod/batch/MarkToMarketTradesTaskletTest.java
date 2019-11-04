package com.ihsmarkit.tfx.eod.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.ihsmarkit.tfx.core.dl.EntityTestDataFactory;
import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodProductCashSettlementRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;
import com.ihsmarkit.tfx.eod.model.MarkToMarketTrade;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceProvider;
import com.ihsmarkit.tfx.eod.service.TradeMtmCalculator;

@ExtendWith(MockitoExtension.class)
class MarkToMarketTradesTaskletTest {

    private static final CurrencyPairEntity CURRENCY_PAIR = EntityTestDataFactory.aCurrencyPairEntityBuilder().build();
    private static final ParticipantEntity PARTICIPANT = EntityTestDataFactory.aParticipantEntityBuilder().build();

    @InjectMocks
    private MarkToMarketTradesTasklet tasklet;

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private DailySettlementPriceProvider dailySettlementPriceProvider;

    @Mock
    private EodProductCashSettlementRepository eodProductCashSettlementRepository;

    @Mock
    private ParticipantPositionRepository participantPositionRepository;

    @Mock
    private TradeMtmCalculator tradeMtmCalculator;

    @Captor
    private ArgumentCaptor<Iterable<EodProductCashSettlementEntity>> captor;

    @Test
    void execute(@Mock StepContribution contribution, @Mock ChunkContext chunkContext) {

        ReflectionTestUtils.setField(tasklet, "businessDateStr", "20191006");

        when(tradeMtmCalculator.calculateAndAggregateInitialMtm(any(), any()))
            .thenReturn(Stream.of(MarkToMarketTrade.of(PARTICIPANT, CURRENCY_PAIR, BigDecimal.ONE)));

        when(tradeMtmCalculator.calculateAndAggregateDailyMtm(any(), any()))
            .thenReturn(Stream.of(MarkToMarketTrade.of(PARTICIPANT, CURRENCY_PAIR, BigDecimal.TEN)));

        final RepeatStatus status = tasklet.execute(contribution, chunkContext);

        assertThat(status).isSameAs(RepeatStatus.FINISHED);

        verify(eodProductCashSettlementRepository, times(2)).saveAll(captor.capture());
        assertThat(captor.getAllValues().stream().flatMap(a -> StreamSupport.stream(a.spliterator(), false)))
            .extracting(
                EodProductCashSettlementEntity::getParticipant,
                EodProductCashSettlementEntity::getCurrencyPair,
                EodProductCashSettlementEntity::getType,
                EodProductCashSettlementEntity::getAmount,
                EodProductCashSettlementEntity::getDate,
                EodProductCashSettlementEntity::getSettlementDate
            )
            .containsExactlyInAnyOrder(
                Tuple.tuple(
                    PARTICIPANT,
                    CURRENCY_PAIR,
                    EodProductCashSettlementType.DAILY_MTM, AmountEntity.of(BigDecimal.TEN, "JPY"),
                    LocalDate.of(2019, 10, 6),
                    LocalDate.of(2019, 10, 9)
                ),
                Tuple.tuple(
                    PARTICIPANT,
                    CURRENCY_PAIR,
                    EodProductCashSettlementType.INITIAL_MTM, AmountEntity.of(BigDecimal.ONE, "JPY"),
                    LocalDate.of(2019, 10, 6),
                    LocalDate.of(2019, 10, 9)
                )
            );

    }
}