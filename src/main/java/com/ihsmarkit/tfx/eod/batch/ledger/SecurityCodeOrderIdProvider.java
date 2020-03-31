package com.ihsmarkit.tfx.eod.batch.ledger;

import java.util.stream.Stream;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.entity.collateral.SecurityCollateralProductEntity;
import com.ihsmarkit.tfx.core.dl.repository.collateral.SecurityCollateralProductRepository;

import lombok.RequiredArgsConstructor;

@Component
@StepScope
@RequiredArgsConstructor
public class SecurityCodeOrderIdProvider extends OrderIdProvider {

    private final SecurityCollateralProductRepository securityCollateralProductRepository;

    @Override
    public Stream<String> loadDataStream() {
        return securityCollateralProductRepository.findAll().stream()
            .map(SecurityCollateralProductEntity::getSecurityCode);
    }
}