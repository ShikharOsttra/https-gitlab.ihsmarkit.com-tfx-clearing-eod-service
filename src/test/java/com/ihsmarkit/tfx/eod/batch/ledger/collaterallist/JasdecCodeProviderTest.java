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
import com.ihsmarkit.tfx.core.dl.entity.custodianaccount.EquityCustodianAccountEntity;
import com.ihsmarkit.tfx.core.dl.repository.CustodianAccountRepository;
import com.ihsmarkit.tfx.core.domain.type.CollateralPurpose;

@ExtendWith(MockitoExtension.class)
class JasdecCodeProviderTest {

    @Mock
    private CustodianAccountRepository custodianAccountRepository;

    @InjectMocks
    private JasdecCodeProvider jasdecCodeProvider;

    @Test
    void shouldReturnJasdecCode() {
        final EquityCustodianAccountEntity account = EntityTestDataFactory
            .anEquityCustodianAccountEntityBuilder()
            .build();

        when(custodianAccountRepository.findAllEquityCustodianAccounts()).thenReturn(List.of(
            account,
            account
        ));

        assertThat(jasdecCodeProvider.getCode(PARTICIPANT_CODE, CollateralPurpose.CLEARING_DEPOSIT)).get().isEqualTo("1234512");
    }

}