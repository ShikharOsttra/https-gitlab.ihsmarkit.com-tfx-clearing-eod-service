package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aFxSpotProductEntity;
import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aParticipantEntityBuilder;
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
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.eod.model.ParticipantCurrencyPairAmount;
import com.ihsmarkit.tfx.eod.model.ledger.TransactionDiary;
import com.ihsmarkit.tfx.eod.service.CurrencyPairSwapPointService;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceService;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.FXSpotProductService;
import com.ihsmarkit.tfx.eod.service.JPYRateService;

@ExtendWith(MockitoExtension.class)
class SODTransactionDiaryLedgerProcessorTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2019, 1, 1);
    private static final LocalDateTime RECORD_DATE = LocalDateTime.of(2019, 1, 2, 11, 30);
    private static final CurrencyPairEntity CURRENCY = CurrencyPairEntity.of(1L, "USD", "JPY");
    private static final ParticipantEntity PARTICIPANT = aParticipantEntityBuilder().build();
    private static final AmountEntity AMOUNT = AmountEntity.builder().value(BigDecimal.TEN).currency("USD").build();
    private static final FxSpotProductEntity FX_SPOT_PRODUCT = aFxSpotProductEntity().build();
    private static final BigDecimal PRICE = BigDecimal.valueOf(123.445);

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
    private ParticipantCurrencyPairAmount participantCurrencyPairAmount;

    @BeforeEach
    void init() {
        this.processor = new SODTransactionDiaryLedgerProcessor(
            BUSINESS_DATE, RECORD_DATE, eodCalculator, currencyPairSwapPointService, jpyRateService, dailySettlementPriceService, fxSpotProductService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void processParticipantPositionEntity() {
        when(fxSpotProductService.getFxSpotProduct(CURRENCY)).thenReturn(FX_SPOT_PRODUCT);
        when(dailySettlementPriceService.getPrice(BUSINESS_DATE, CURRENCY)).thenReturn(BigDecimal.TEN);
        when(eodCalculator.calculateSwapPoint(any(ParticipantPositionEntity.class), any(Function.class), any(Function.class)))
            .thenReturn(participantCurrencyPairAmount);
        when(eodCalculator.calculateDailyMtmValue(any(ParticipantPositionEntity.class), any(Function.class), any(Function.class)))
            .thenReturn(participantCurrencyPairAmount);
        when(participantCurrencyPairAmount.getAmount()).thenReturn(BigDecimal.ZERO);

        assertThat(processor.process(aParticipantPosition()))
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
                .tradePrice(PRICE.toPlainString())
                .sellAmount(EMPTY)
                .buyAmount(EMPTY)
                .counterpartyCode(EMPTY)
                .counterpartyType(EMPTY)
                .dsp("10")
                .dailyMtMAmount("0")
                .swapPoint("0")
                .outstandingPositionAmount(EMPTY)
                .settlementDate("2019/01/02")
                .tradeId(EMPTY)
                .tradeType(EMPTY)
                .reference(EMPTY)
                .userReference(EMPTY)
                .build());
    }

    private ParticipantPositionEntity aParticipantPosition() {
        return ParticipantPositionEntity.builder()
            .participant(PARTICIPANT)
            .currencyPair(CURRENCY)
            .amount(AMOUNT)
            .valueDate(BUSINESS_DATE.plusDays(1))
            .tradeDate(BUSINESS_DATE)
            .price(PRICE)
            .build();
    }
}