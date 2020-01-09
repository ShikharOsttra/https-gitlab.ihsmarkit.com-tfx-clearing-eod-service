package com.ihsmarkit.tfx.eod.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ihsmarkit.tfx.core.dl.entity.eod.EodStage;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodStatusCompositeId;
import com.ihsmarkit.tfx.core.dl.repository.SystemParameterRepository;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.core.dl.repository.calendar.CalendarTradingSwapPointRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodDataRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodStatusRepository;
import com.ihsmarkit.tfx.core.domain.type.SystemParameters;
import com.ihsmarkit.tfx.core.domain.type.TransactionType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EODControlService {

    private final SystemParameterRepository systemParameterRepository;
    private final EodStatusRepository eodStatusRepository;
    private final TradeRepository tradeRepository;
    private final CalendarTradingSwapPointRepository calendarRepository;

    private final List<EodDataRepository> eodDataRepositories;

    @Transactional
    public LocalDate undoLatestEODResults() {

        final LocalDate currentBusinessDate = getCurrentBusinessDate();
        final LocalDate previousBusinessDate = getPreviousBusinessDate(currentBusinessDate);

        tradeRepository.deleteAllByTransactionTypeAndTradeDate(TransactionType.BALANCE, currentBusinessDate);
        eodDataRepositories.forEach(repository -> repository.deleteAllByDate(currentBusinessDate));
        removeEODStageRecordsForDate(currentBusinessDate);

        setCurrentBusinessDate(previousBusinessDate);

        return getCurrentBusinessDate();
    }

    private void removeEODStageRecordsForDate(LocalDate businessDate) {
        final EodStatusCompositeId eod1CompleteStatus = new EodStatusCompositeId(EodStage.EOD1_COMPLETE, businessDate);
        final EodStatusCompositeId eod2CompleteStatus = new EodStatusCompositeId(EodStage.EOD2_COMPLETE, businessDate);
        final EodStatusCompositeId dspApprovedStatus = new EodStatusCompositeId(EodStage.DSP_APPROVED, businessDate);
        final EodStatusCompositeId swapPointsApprovedStatus = new EodStatusCompositeId(EodStage.SWAP_POINTS_APPROVED, businessDate);
        if (eodStatusRepository.existsById(eod1CompleteStatus)) {
            eodStatusRepository.deleteById(eod1CompleteStatus);
        }
        if (eodStatusRepository.existsById(eod2CompleteStatus)) {
            eodStatusRepository.deleteById(eod2CompleteStatus);
        }
        if (eodStatusRepository.existsById(dspApprovedStatus)) {
            eodStatusRepository.deleteById(dspApprovedStatus);
        }
        if (eodStatusRepository.existsById(swapPointsApprovedStatus)) {
            eodStatusRepository.deleteById(swapPointsApprovedStatus);
        }
    }

    private LocalDate getCurrentBusinessDate() {
        return systemParameterRepository.getParameterValueFailFast(SystemParameters.BUSINESS_DATE);
    }

    private void setCurrentBusinessDate(LocalDate businessDate) {
        systemParameterRepository.setParameter(SystemParameters.BUSINESS_DATE, businessDate);
    }

    private LocalDate getPreviousBusinessDate(LocalDate currentBusinessDate) {
        return calendarRepository.findPrevBankBusinessDate(currentBusinessDate).orElse(currentBusinessDate);
    }
}
