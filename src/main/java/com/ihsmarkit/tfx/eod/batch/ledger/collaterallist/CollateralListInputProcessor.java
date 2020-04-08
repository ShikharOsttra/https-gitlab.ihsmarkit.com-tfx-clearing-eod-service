package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralBalanceEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.SecurityCollateralProductEntity;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.domain.CollateralListItem;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@StepScope
public class CollateralListInputProcessor implements ItemProcessor<CollateralBalanceEntity, CollateralListItem> {

    private final CollateralCalculator collateralCalculator;

    @Override
    public CollateralListItem process(final CollateralBalanceEntity balance) {
        return CollateralListItem.builder()
            .balance(balance)
            .evaluatedPrice(balance.getProduct() instanceof SecurityCollateralProductEntity
                            ? collateralCalculator.calculateEvaluatedPrice((SecurityCollateralProductEntity) balance.getProduct())
                            : null
            )
            .evaluatedAmount(collateralCalculator.calculateEvaluatedAmount(balance))
            .build();
    }

}
