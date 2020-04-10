package com.ihsmarkit.tfx.eod.service;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.ihsmarkit.tfx.core.domain.type.SystemParameters.BUSINESS_DATE;

import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.data.util.Lazy;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.FxSpotProductEntity;
import com.ihsmarkit.tfx.core.dl.entity.FxSpotTickSizeEntity;
import com.ihsmarkit.tfx.core.dl.repository.FxSpotProductRepository;
import com.ihsmarkit.tfx.core.dl.repository.FxSpotTickSizeRepository;
import com.ihsmarkit.tfx.core.dl.repository.SystemParameterRepository;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Component
@StepScope
@RequiredArgsConstructor
@Getter(AccessLevel.PRIVATE)
public class FXSpotProductService {

    private final FxSpotProductRepository fxSpotProductRepository;
    private final FxSpotTickSizeRepository fxSpotTickSizeRepository;
    private final SystemParameterRepository systemParameterRepository;

    private final Lazy<Map<String, FxSpotProductEntity>> fxSpotProductCache = Lazy.of(
        () -> getFxSpotProductRepository().findAll().stream()
            .collect(Collectors.toMap(item -> item.getCurrencyPair().getCode(), Function.identity()))
    );

    private final Lazy<Map<String, FxSpotTickSizeEntity>> fxSpotTickSizeCache = Lazy.of(
        () -> getFxSpotTickSizeRepository().findByBusinessDate(getBusinessDate()).stream()
            .collect(Collectors.toMap(item -> item.getFxSpotProduct().getCurrencyPair().getCode(), Function.identity()))
    );

    public FxSpotProductEntity getFxSpotProduct(final CurrencyPairEntity key) {
        return getFxSpotProduct(key.getCode());
    }

    public FxSpotProductEntity getFxSpotProduct(final String currencyPairCode) {
        return fxSpotProductCache.get().get(currencyPairCode);
    }

    public int getScaleForCurrencyPair(final String currencyPairCode) {
        final FxSpotTickSizeEntity tickSize = checkNotNull(fxSpotTickSizeCache.get().get(currencyPairCode), "No Tick size for currency " + currencyPairCode);

        return tickSize.getValue().stripTrailingZeros()
            .scale();
    }

    public int getScaleForCurrencyPair(final CurrencyPairEntity currencyPair) {
        return getScaleForCurrencyPair(currencyPair.getCode());
    }

    private LocalDate getBusinessDate() {
        return systemParameterRepository.getParameterValueFailFast(BUSINESS_DATE);
    }
}
