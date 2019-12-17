package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.repository.CustodianAccountRepository;

@Service
@StepScope
public class JasdecCodeProvider extends AbstractCustodianAccountCodeProvider {

    public JasdecCodeProvider(final CustodianAccountRepository custodianAccountRepository) {
        super(custodianAccountRepository::findAllEquityCustodianAccounts, account -> account.getInstitutionCode() + account.getAccountType());
    }

}
