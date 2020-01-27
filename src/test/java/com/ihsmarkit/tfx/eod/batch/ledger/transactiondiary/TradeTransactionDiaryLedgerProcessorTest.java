package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aFxSpotProductEntity;
import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aParticipantEntityBuilder;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantType.FX_BROKER;
import static com.ihsmarkit.tfx.core.domain.type.TransactionType.REGULAR;
import static org.apache.logging.log4j.util.Strings.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.FxSpotProductEntity;
import com.ihsmarkit.tfx.core.dl.entity.LegalEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.domain.type.Side;
import com.ihsmarkit.tfx.core.time.ClockService;
import com.ihsmarkit.tfx.eod.model.ParticipantCurrencyPairAmount;
import com.ihsmarkit.tfx.eod.model.ledger.TransactionDiary;
import com.ihsmarkit.tfx.eod.service.CurrencyPairSwapPointService;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceService;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.FXSpotProductService;
import com.ihsmarkit.tfx.eod.service.JPYRateService;

@ExtendWith(MockitoExtension.class)
class TradeTransactionDiaryLedgerProcessorTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2019, 1, 1);
    private static final LocalDateTime RECORD_DATE = LocalDateTime.of(2019, 1, 2, 11, 30);
    private static final LocalDateTime MATCHING_DATE = LocalDateTime.of(2019, 1, 1, 1, 30);
    private static final CurrencyPairEntity CURRENCY = CurrencyPairEntity.of(1L, "USD", "JPY");
    private static final ParticipantEntity PARTICIPANT = aParticipantEntityBuilder().build();
    private static final AmountEntity AMOUNT = AmountEntity.builder().value(BigDecimal.TEN).currency("USD").build();
    private static final FxSpotProductEntity FX_SPOT_PRODUCT = aFxSpotProductEntity().build();

    private TradeTransactionDiaryLedgerProcessor processor;
    @Mock
    private DailySettlementPriceService dailySettlementPriceService;
    @Mock
    private FXSpotProductService fxSpotProductService;
    @Mock
    private EODCalculator eodCalculator;
    @Mock
    private JPYRateService jpyRateService;
    @Mock
    private ParticipantCurrencyPairAmount participantCurrencyPairAmount;
    @Mock
    private LegalEntity originator;
    @Mock
    private LegalEntity counterparty;
    @Mock
    private ClockService clockService;
    @Mock
    private CurrencyPairSwapPointService currencyPairSwapPointService;

    @BeforeEach
    void init() {
        this.processor = new TradeTransactionDiaryLedgerProcessor(
            BUSINESS_DATE, RECORD_DATE, eodCalculator, jpyRateService, dailySettlementPriceService, fxSpotProductService, clockService,
            currencyPairSwapPointService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void processTradeEntity() {
        when(clockService.utcTimeToServerTime(any())).thenReturn(MATCHING_DATE);

        when(fxSpotProductService.getFxSpotProduct(CURRENCY)).thenReturn(FX_SPOT_PRODUCT);
        when(dailySettlementPriceService.getPrice(BUSINESS_DATE, CURRENCY)).thenReturn(BigDecimal.TEN);
        when(eodCalculator.calculateInitialMtmValue(any(TradeEntity.class), any(Function.class), any(Function.class)))
            .thenReturn(participantCurrencyPairAmount);
        when(eodCalculator.calculateSwapPoint(any(TradeEntity.class), any(Function.class), any(Function.class)))
            .thenReturn(participantCurrencyPairAmount);
        when(participantCurrencyPairAmount.getAmount()).thenReturn(BigDecimal.ZERO);
        when(originator.getParticipant()).thenReturn(PARTICIPANT);
        when(counterparty.getParticipant()).thenReturn(aParticipantEntityBuilder()
            .code("counterparty")
            .type(FX_BROKER)
            .build());

        assertThat(processor.process(aTrade()))
            .containsExactly(TransactionDiary.builder()
                .businessDate(BUSINESS_DATE)
                .tradeDate("2019/01/01")
                .recordDate("2019/01/02 11:30:00")
                .participantCode("BNP")
                .participantName("BNP Paribas Securities(Japan) Limited")
                .participantType("LP")
                .currencyNo("101")
                .currencyPair("USD/JPY")
                .matchDate("2019/01/01")
                .matchTime("01:30:00")
                .matchId("matchingRef")
                .clearDate("2019/01/01")
                .clearTime("01:30:00")
                .clearingId("clearingRef")
                .tradePrice("10")
                .sellAmount("10")
                .buyAmount(EMPTY)
                .counterpartyCode("counterparty")
                .counterpartyType("FXB")
                .dsp("10")
                .dailyMtMAmount("0")
                .swapPoint("0")
                .outstandingPositionAmount(EMPTY)
                .settlementDate("2019/01/02")
                .tradeId("tradeRef")
                .tradeType("1")
                .reference(EMPTY)
                .userReference(EMPTY)
                .build());
    }

    private TradeEntity aTrade() {
        return TradeEntity.builder()
            .originator(originator)
            .counterparty(counterparty)
            .currencyPair(CURRENCY)
            .matchingTsp(MATCHING_DATE)
            .matchingRef("matchingRef")
            .clearingTsp(MATCHING_DATE)
            .clearingRef("clearingRef")
            .spotRate(BigDecimal.TEN)
            .baseAmount(AMOUNT)
            .direction(Side.SELL)
            .valueDate(BUSINESS_DATE.plusDays(1))
            .tradeReference("tradeRef")
            .transactionType(REGULAR)
            .build();
    }
}