package com.ihsmarkit.tfx.eod.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ihsmarkit.tfx.core.dl.EntityTestDataFactory;
import com.ihsmarkit.tfx.core.dl.entity.marketdata.DailySettlementPriceEntity;
import com.ihsmarkit.tfx.core.dl.repository.marketdata.DailySettlementPriceRepository;

@ExtendWith(MockitoExtension.class)
class DailySettlementPriceServiceTest {

    @Mock
    private DailySettlementPriceRepository dailySettlementPriceRepository;
    @InjectMocks
    private DailySettlementPriceService dailySettlementPriceService;

    @Test
    void shouldGetPrice() {
        final var currencyPair = EntityTestDataFactory.aCurrencyPairEntityBuilder().build();
        when(dailySettlementPriceRepository.findLatestDailySettlementPrices(any())).thenReturn(
            List.of(DailySettlementPriceEntity.builder()
                    .currencyPair(currencyPair)
                    .dailySettlementPrice(BigDecimal.ONE)
                    .build(),
                DailySettlementPriceEntity.builder()
                    .currencyPair(currencyPair)
                    .build()
            )
        );
        final var actualPrice = dailySettlementPriceService.getPrice(LocalDate.MIN, currencyPair);
        assertThat(actualPrice).isEqualByComparingTo(BigDecimal.ONE);
    }
}