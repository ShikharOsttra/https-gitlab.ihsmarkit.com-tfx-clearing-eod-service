package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.data.util.Lazy;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.FxSpotProductEntity;
import com.ihsmarkit.tfx.core.dl.repository.FxSpotProductRepository;

@Service
@StepScope
public class FxSpotProductQueryProvider {

    private final Lazy<Map<CurrencyPairEntity, String>> productNumbers;

    public FxSpotProductQueryProvider(final FxSpotProductRepository fxSpotProductRepository) {
        this.productNumbers = Lazy.of(() -> fxSpotProductRepository.findAll()
            .stream()
            .collect(Collectors.toMap(FxSpotProductEntity::getCurrencyPair, FxSpotProductEntity::getProductNumber)));
    }

    public String getCurrencyNo(final CurrencyPairEntity currencyPairEntity) {
        return productNumbers.get().get(currencyPairEntity);
    }
}
