package com.ihsmarkit.tfx.eod.controller;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.CASH_BALANCE_UPDATE_BATCH_JOB_NAME;

import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.batch.core.BatchStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.ihsmarkit.tfx.eod.api.EodServiceControllerApi;
import com.ihsmarkit.tfx.eod.service.EODControlService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class EODServiceController implements EodServiceControllerApi {

    private final EODControlService eodControlService;

    @Override
    public ResponseEntity<LocalDate> undoCurrentEOD(@NotNull @Valid final Boolean keepMarketData) {
        return ResponseEntity.ok(eodControlService.undoCurrentDayEOD(keepMarketData));
    }

    @Override
    public ResponseEntity<LocalDate> undoPreviousEOD(@NotNull @Valid final Boolean keepMarketData) {
        return ResponseEntity.ok(eodControlService.undoPreviousDayEOD(keepMarketData));
    }

    @Override
    public ResponseEntity<LocalDate> getCurrentBusinessDay() {
        return ResponseEntity.ok(eodControlService.getCurrentBusinessDate());
    }

    @Override
    public ResponseEntity<String> runEOD1() {
        return ResponseEntity.ok(eodControlService.runEOD1Job().name());
    }

    @Override
    public ResponseEntity<String> runEOD2() {
        return ResponseEntity.ok(eodControlService.runEOD2Job().name());
    }

    @Override
    public ResponseEntity<LocalDate> rollBusinessDate() {
        eodControlService.rollBusinessDateJob();
        return getCurrentBusinessDay();
    }

    @Override
    public ResponseEntity<LocalDate> runAll() {
        if (eodControlService.runEOD1Job() == BatchStatus.COMPLETED
            && eodControlService.runEOD2Job() == BatchStatus.COMPLETED
            && eodControlService.rollBusinessDateJob() == BatchStatus.COMPLETED) {
            return getCurrentBusinessDay();
        }
        return ResponseEntity.ok(LocalDate.MIN);
    }

    @Override
    public ResponseEntity<String> updateCashBalances() {
        return ResponseEntity.ok(eodControlService.runJob(CASH_BALANCE_UPDATE_BATCH_JOB_NAME).name());
    }
}
