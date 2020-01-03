package com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.data.util.Lazy;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.entity.FxSpotProductEntity;
import com.ihsmarkit.tfx.core.dl.repository.FxSpotProductRepository;

@Component
@JobScope
public class FxSpotProductDataProvider {

    private final Lazy<Map<String, FxSpotProductEntity>> fxSpotProductMap;

    public FxSpotProductDataProvider(final FxSpotProductRepository fxSpotProductRepository) {
        fxSpotProductMap = Lazy.of(() -> fxSpotProductRepository.findAllOrderByProductNumberAsc().stream()
                .collect(Collectors.toMap(
                    item -> item.getCurrencyPair().getCode(),
                    Function.identity()
                )));
    }

    public BigDecimal getTradingUnit(final String currencyPairCode) {
        return new BigDecimal(fxSpotProductMap.get().get(currencyPairCode).getTradingUnit());
    }

    public String getProductNumber(final String currencyPairCode) {
        return fxSpotProductMap.get().get(currencyPairCode).getProductNumber();
    }

}
