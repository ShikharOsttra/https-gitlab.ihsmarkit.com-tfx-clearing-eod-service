package com.ihsmarkit.tfx.eod.service;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.data.util.Lazy;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.FxSpotProductEntity;
import com.ihsmarkit.tfx.core.dl.repository.FxSpotProductRepository;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Component
@JobScope
@RequiredArgsConstructor
@Getter(AccessLevel.PRIVATE)
public class FXSpotProductService {

    private final FxSpotProductRepository fxSpotProductRepository;

    private final Lazy<Map<CurrencyPairEntity, FxSpotProductEntity>> cache = Lazy.of(
        () -> getFxSpotProductRepository().findAll().stream()
            .collect(Collectors.toMap(FxSpotProductEntity::getCurrencyPair, Function.identity()))
    );

    public FxSpotProductEntity getFxSpotProduct(final CurrencyPairEntity key) {
        return cache.get().get(key);
    }
}
