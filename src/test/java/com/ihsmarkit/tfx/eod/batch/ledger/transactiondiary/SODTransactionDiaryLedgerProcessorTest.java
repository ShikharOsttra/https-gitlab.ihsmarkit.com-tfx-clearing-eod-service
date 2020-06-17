package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aFxSpotProductEntity;
import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aParticipantEntityBuilder;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.SOD;
import static org.apache.logging.log4j.util.Strings.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.FxSpotProductEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;
import com.ihsmarkit.tfx.eod.model.ParticipantCurrencyPairAmount;
import com.ihsmarkit.tfx.eod.model.ledger.TransactionDiary;
import com.ihsmarkit.tfx.eod.service.CalendarDatesProvider;
import com.ihsmarkit.tfx.eod.service.CurrencyPairSwapPointService;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceService;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.FXSpotProductService;
import com.ihsmarkit.tfx.eod.service.JPYRateService;
import com.ihsmarkit.tfx.eod.service.TradeAndSettlementDateService;

@ExtendWith(MockitoExtension.class)
class SODTransactionDiaryLedgerProcessorTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2019, 1, 1);
    private static final LocalDateTime RECORD_DATE = LocalDateTime.of(2019, 1, 2, 11, 30);
    private static final CurrencyPairEntity CURRENCY_PAIR = CurrencyPairEntity.of(1L, "USD", "JPY");
    private static final ParticipantEntity PARTICIPANT = aParticipantEntityBuilder().build();
    private static final AmountEntity AMOUNT = AmountEntity.builder().value(BigDecimal.TEN).currency("USD").build();
    private static final FxSpotProductEntity FX_SPOT_PRODUCT = aFxSpotProductEntity().build();
    private static final ParticipantCurrencyPairAmount PARTICIPANT_CURRENCY_PAIR_AMOUNT =
        ParticipantCurrencyPairAmount.of(PARTICIPANT, CURRENCY_PAIR, BigDecimal.ZERO);
    private static final BigDecimal PRICE = BigDecimal.valueOf(123.445);
    private static final long ORDER_ID = 11010000000000L;

    private SODTransactionDiaryLedgerProcessor processor;
    @Mock
    private DailySettlementPriceService dailySettlementPriceService;
    @Mock
    private FXSpotProductService fxSpotProductService;
    @Mock
    private EODCalculator eodCalculator;
    @Mock
    private CurrencyPairSwapPointService currencyPairSwapPointService;
    @Mock
    private JPYRateService jpyRateService;
    @Mock
    private TransactionDiaryOrderIdProvider transactionDiaryOrderIdProvider;
    @Mock
    private ParticipantPositionRepository participantPositionRepository;
    @Mock
    private TradeAndSettlementDateService tradeAndSettlementDateService;
    @Mock
    private PositionDateProvider positionDateProvider;
    @Mock
    private CalendarDatesProvider calendarDatesProvider;

    @BeforeEach
    void init() {
        this.processor = new SODTransactionDiaryLedgerProcessor(
            BUSINESS_DATE, RECORD_DATE, eodCalculator, currencyPairSwapPointService, jpyRateService, dailySettlementPriceService, fxSpotProductService,
            transactionDiaryOrderIdProvider, participantPositionRepository, tradeAndSettlementDateService, positionDateProvider, calendarDatesProvider);
    }

    @Test
    @SuppressWarnings("unchecked")
    void processParticipantPositionEntity() {
        when(fxSpotProductService.getFxSpotProduct(CURRENCY_PAIR)).thenReturn(FX_SPOT_PRODUCT);
        when(dailySettlementPriceService.getPrice(BUSINESS_DATE, CURRENCY_PAIR)).thenReturn(new BigDecimal("10.000100"));
        when(eodCalculator.calculateSwapPoint(any(ParticipantPositionEntity.class), any(Function.class), any(Function.class)))
            .thenReturn(PARTICIPANT_CURRENCY_PAIR_AMOUNT);
        when(eodCalculator.calculateDailyMtmValue(any(ParticipantPositionEntity.class), any(Function.class), any(Function.class)))
            .thenReturn(PARTICIPANT_CURRENCY_PAIR_AMOUNT);
        when(fxSpotProductService.getScaleForCurrencyPair(any(CurrencyPairEntity.class))).thenReturn(5);
        when(transactionDiaryOrderIdProvider.getOrderId(PARTICIPANT.getCode(), FX_SPOT_PRODUCT.getProductNumber(), '0')).thenReturn(ORDER_ID);
        when(participantPositionRepository.findByPositionTypeAndTradeDateAndCurrencyPairAndParticipantFetchAll(
            SOD,
            BUSINESS_DATE,
            CURRENCY_PAIR,
            PARTICIPANT
        )).thenReturn(Optional.of(aParticipantPosition()));
        when(positionDateProvider.getSodDate()).thenReturn(LocalDateTime.of(2019, 1, 1, 7, 0));
        when(calendarDatesProvider.getSettlementDate(CURRENCY_PAIR)).thenReturn(LocalDate.of(2019, 1, 3));

        assertThat(processor.process(new ParticipantAndCurrencyPair(PARTICIPANT, CURRENCY_PAIR)))
            .isEqualTo(TransactionDiary.builder()
                .businessDate(BUSINESS_DATE)
                .tradeDate("2019/01/01")
                .recordDate("2019/01/02 11:30:00")
                .participantCode("BNP")
                .participantName("BNP Paribas Securities(Japan) Limited")
                .participantType("LP")
                .currencyNo("101")
                .currencyPair("USD/JPY")
                .matchDate("2019/01/01")
                .matchTime("07:00:00")
                .matchId(EMPTY)
                .clearDate("2019/01/01")
                .clearTime("07:00:00")
                .clearingId(EMPTY)
                .tradePrice("123.44500")
                .sellAmount(EMPTY)
                .buyAmount("10")
                .counterpartyCode(EMPTY)
                .counterpartyType(EMPTY)
                .dsp("10.00010")
                .dailyMtMAmount("0")
                .swapPoint("0")
                .outstandingPositionAmount("0")
                .settlementDate("2019/01/03")
                .tradeId(EMPTY)
                .reference(EMPTY)
                .userReference(EMPTY)
                .orderId(ORDER_ID)
                .build());
    }

    private ParticipantPositionEntity aParticipantPosition() {
        return ParticipantPositionEntity.builder()
            .participant(PARTICIPANT)
            .currencyPair(CURRENCY_PAIR)
            .amount(AMOUNT)
            .valueDate(BUSINESS_DATE.plusDays(1))
            .tradeDate(BUSINESS_DATE)
            .price(PRICE)
            .build();
    }
}