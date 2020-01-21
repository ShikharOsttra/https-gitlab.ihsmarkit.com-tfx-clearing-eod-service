package com.ihsmarkit.tfx.eod.batch.ledger.marketdata;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aFxSpotProductEntity;
import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aParticipantEntityBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.entity.marketdata.DailySettlementPriceEntity;
import com.ihsmarkit.tfx.core.dl.repository.FxSpotProductRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.dl.repository.marketdata.DailySettlementPriceRepository;
import com.ihsmarkit.tfx.core.time.ClockService;
import com.ihsmarkit.tfx.eod.model.ledger.DailyMarkedDataAggregated;
import com.ihsmarkit.tfx.eod.model.ledger.DailyMarketDataEnriched;
import com.ihsmarkit.tfx.eod.service.CurrencyPairSwapPointService;

import lombok.SneakyThrows;

@ExtendWith(MockitoExtension.class)
class DailyMarketDataProcessorTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2019, 1, 1);
    private static final LocalDateTime RECORD_DATE = LocalDateTime.of(BUSINESS_DATE, LocalTime.of(1, 1));
    private static final ParticipantEntity PARTICIPANT = aParticipantEntityBuilder().build();

    @Mock
    private CurrencyPairSwapPointService currencyPairSwapPointService;
    @Mock
    private DailySettlementPriceRepository dailySettlementPriceRepository;
    @Mock
    private FxSpotProductRepository fxSpotProductRepository;
    @Mock
    private ParticipantPositionRepository participantPositionRepository;
    @Mock
    private ClockService clockService;

    private DailyMarketDataProcessor aggregator;

    @BeforeEach
    void beforeEach() {
        when(clockService.getServerZoneOffset()).thenReturn(ZoneOffset.UTC);
        aggregator = new DailyMarketDataProcessor(
            currencyPairSwapPointService, dailySettlementPriceRepository, fxSpotProductRepository,
            participantPositionRepository, clockService, BUSINESS_DATE, RECORD_DATE
        );
    }

    @Test
    @SneakyThrows
    void shouldAggregateAllRecords() {
        mockSwapPoints();
        mockDsp();
        mockOpenPositionAmount();
        mockTradingUnit();

        assertThat(aggregator.process(getInputData()))
            .isNotEmpty()
            .extracting(
                DailyMarketDataEnriched::getCurrencyPairCode,
                DailyMarketDataEnriched::getBusinessDate,
                DailyMarketDataEnriched::getDspChange,
                DailyMarketDataEnriched::getSwapPoint,
                DailyMarketDataEnriched::getOpenPositionAmount
            )
            .containsExactlyInAnyOrder(
                tuple("USD/JPY", BUSINESS_DATE, "0.0600000", "1", "0"),
                tuple("EUR/JPY", BUSINESS_DATE, "0.0000000", "1", "4444")
            );
    }

    private void mockSwapPoints() {
        when(currencyPairSwapPointService.getSwapPoint(any(), anyString()))
            .thenReturn(BigDecimal.ONE);
    }

    private void mockDsp() {
        when(dailySettlementPriceRepository.findAllByBusinessDate(any()))
            .thenReturn(List.of(
                DailySettlementPriceEntity.builder()
                    .currencyPair(currencyPair("USD", "JPY"))
                    .dailySettlementPrice(new BigDecimal("1.5100000"))
                    .previousDailySettlementPrice(new BigDecimal("1.4500000"))
                    .build(),
                DailySettlementPriceEntity.builder()
                    .currencyPair(currencyPair("EUR", "JPY"))
                    .dailySettlementPrice(new BigDecimal("2.3000000"))
                    .previousDailySettlementPrice(new BigDecimal("2.3000000"))
                    .build()
            ));
    }

    private void mockOpenPositionAmount() {
        when(participantPositionRepository.findAllByPositionTypeAndTradeDateFetchCurrencyPair(any(), any()))
            .thenReturn(List.of(
                ParticipantPositionEntity.builder()
                    .currencyPair(currencyPair("USD", "JPY"))
                    .amount(AmountEntity.of(new BigDecimal("1111"), "JPY"))
                    .participant(PARTICIPANT)
                    .build(),
                ParticipantPositionEntity.builder()
                    .currencyPair(currencyPair("EUR", "JPY"))
                    .amount(AmountEntity.of(new BigDecimal("-2222"), "JPY"))
                    .participant(PARTICIPANT)
                    .build()
            ));
    }

    private void mockTradingUnit() {
        when(fxSpotProductRepository.findAllOrderByProductNumberAsc())
            .thenReturn(List.of(
                aFxSpotProductEntity()
                    .currencyPair(currencyPair("USD", "JPY"))
                    .tradingUnit(1000L)
                    .build(),
                aFxSpotProductEntity()
                    .currencyPair(currencyPair("EUR", "JPY"))
                    .tradingUnit(1000L)
                    .build()
            ));
    }

    private static Map<String, DailyMarkedDataAggregated> getInputData() {
        return Map.of(
            "USD/JPY", DailyMarkedDataAggregated.of(
                "USD/JPY", new BigDecimal("1011.3"),
                new BigDecimal("1.1"), LocalDateTime.of(BUSINESS_DATE, LocalTime.of(1, 1)),
                new BigDecimal("1000.1"), LocalDateTime.of(BUSINESS_DATE, LocalTime.of(1, 2)),
                new BigDecimal("1.1"), LocalDateTime.of(BUSINESS_DATE, LocalTime.of(1, 1)),
                new BigDecimal("10.1"), LocalDateTime.of(BUSINESS_DATE, LocalTime.of(1, 3))
            ),
            "EUR/JPY", DailyMarkedDataAggregated.of(
                "EUR/JPY", new BigDecimal("117.2"),
                new BigDecimal("17.1"), LocalDateTime.of(BUSINESS_DATE, LocalTime.of(1, 3)),
                new BigDecimal("100.1"), LocalDateTime.of(BUSINESS_DATE, LocalTime.of(1, 3)),
                new BigDecimal("17.1"), LocalDateTime.of(BUSINESS_DATE, LocalTime.of(1, 3)),
                new BigDecimal("100.1"), LocalDateTime.of(BUSINESS_DATE, LocalTime.of(1, 3))
            )
        );
    }

    private static CurrencyPairEntity currencyPair(final String baseCurrency, final String valueCurrency) {
        return CurrencyPairEntity.of(1L, baseCurrency, valueCurrency);
    }

}