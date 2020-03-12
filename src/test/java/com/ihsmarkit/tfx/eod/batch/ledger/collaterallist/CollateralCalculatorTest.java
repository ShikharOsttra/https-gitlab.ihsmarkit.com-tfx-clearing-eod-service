package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ihsmarkit.tfx.collateral.calculator.CollateralCalculatorFactory;
import com.ihsmarkit.tfx.collateral.calculator.domain.ProductDetailsAdapter;
import com.ihsmarkit.tfx.core.dl.CollateralTestDataFactory;
import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralBalanceEntity;

@ExtendWith(MockitoExtension.class)
class CollateralCalculatorTest {


    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CollateralCalculatorFactory collateralCalculatorFactory;

    @InjectMocks
    private CollateralCalculator collateralCalculator;

    @Test
    void shouldReturnTrimmedEvaluatedAmount() {
        final CollateralBalanceEntity balance = mock(CollateralBalanceEntity.class);
        when(balance.getAmount()).thenReturn(BigDecimal.TEN);
        when(balance.getProduct()).thenReturn(CollateralTestDataFactory.aBondCollateralProductEntityBuilder().build());

        when(collateralCalculatorFactory.getEvaluatedAmountCalculator().calculate(eq(BigDecimal.TEN), any(ProductDetailsAdapter.class)))
            .thenReturn(BigDecimal.valueOf(123.13));

        assertThat(collateralCalculator.calculateEvaluatedAmount(balance)).isEqualTo(BigDecimal.valueOf(123));
    }

}