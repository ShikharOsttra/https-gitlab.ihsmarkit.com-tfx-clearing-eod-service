package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.PARTICIPANT_CODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ihsmarkit.tfx.core.dl.EntityTestDataFactory;
import com.ihsmarkit.tfx.core.dl.repository.CustodianAccountRepository;
import com.ihsmarkit.tfx.core.domain.type.CollateralPurpose;

@ExtendWith(MockitoExtension.class)
class BojCodeProviderTest {

    @Mock
    private CustodianAccountRepository custodianAccountRepository;

    @InjectMocks
    private BojCodeProvider bojCodeProvider;

    @Test
    void shouldReturnBojCode() {
        when(custodianAccountRepository.findAllBondCustodianAccounts()).thenReturn(List.of(
            EntityTestDataFactory.aBondCustodianAccountEntityBuilder().build()
        ));

        assertThat(bojCodeProvider.getCode(PARTICIPANT_CODE, CollateralPurpose.MARKET_ENTRY_DEPOSIT)).get().isEqualTo("0001");
    }

    @Test
    void shouldReturnEmptyOnMultipleResults() {
        when(custodianAccountRepository.findAllBondCustodianAccounts()).thenReturn(List.of(
            EntityTestDataFactory.aBondCustodianAccountEntityBuilder().build(),
            EntityTestDataFactory.aBondCustodianAccountEntityBuilder().build()
        ));

        assertThat(bojCodeProvider.getCode(PARTICIPANT_CODE, CollateralPurpose.MARKET_ENTRY_DEPOSIT)).isEmpty();
    }

}