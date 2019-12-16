package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.custodianaccount.BondCustodianAccountEntity;
import com.ihsmarkit.tfx.core.dl.repository.CustodianAccountRepository;

@Service
@StepScope
public class BojCodeProvider extends AbstractCustodianAccountCodeProvider {

    public BojCodeProvider(final CustodianAccountRepository custodianAccountRepository) {
        super(custodianAccountRepository::findAllBondCustodianAccounts, BondCustodianAccountEntity::getBojParticipantCode);
    }

}
