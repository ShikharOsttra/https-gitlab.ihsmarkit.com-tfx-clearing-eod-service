package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aFxSpotProductEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.repository.FxSpotProductRepository;

@ExtendWith(MockitoExtension.class)
class FxSpotProductQueryProviderTest {

    private static final CurrencyPairEntity CURRENCY = CurrencyPairEntity.of(1L, "USD", "JPY");

    @Mock
    private FxSpotProductRepository repository;

    private FxSpotProductQueryProvider provider;

    @BeforeEach
    void init() {
        provider = new FxSpotProductQueryProvider(repository);
    }

    @Test
    void getCurrencyNo() {
        when(repository.findAll()).thenReturn(
            List.of(aFxSpotProductEntity()
                .currencyPair(CURRENCY)
                .productNumber("101")
                .build()));

        assertThat(provider.getCurrencyNo(CURRENCY))
            .isEqualTo("101");
    }
}