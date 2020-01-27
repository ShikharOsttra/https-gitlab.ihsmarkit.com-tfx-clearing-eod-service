package com.ihsmarkit.tfx.eod.controller;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.CASH_BALANCE_UPDATE_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD1_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD2_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.ROLL_BUSINESS_DATE_JOB_NAME;

import java.time.LocalDate;
import java.util.stream.Stream;

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
        return ResponseEntity.ok(eodControlService.runEODJob(EOD1_BATCH_JOB_NAME).name());
    }

    @Override
    public ResponseEntity<String> runEOD2() {
        return ResponseEntity.ok(eodControlService.runEODJob(EOD2_BATCH_JOB_NAME).name());
    }

    @Override
    public ResponseEntity<LocalDate> rollBusinessDate() {
        eodControlService.runEODJob(ROLL_BUSINESS_DATE_JOB_NAME);
        return getCurrentBusinessDay();
    }

    @Override
    public ResponseEntity<LocalDate> runAll() {
        Stream.of(EOD1_BATCH_JOB_NAME, EOD2_BATCH_JOB_NAME, ROLL_BUSINESS_DATE_JOB_NAME)
            .filter(job -> eodControlService.runEODJob(job) != BatchStatus.COMPLETED)
            .findFirst();
        return getCurrentBusinessDay();
    }

    public ResponseEntity<String> updateCashBalances() {
        return ResponseEntity.ok(eodControlService.runEODJob(CASH_BALANCE_UPDATE_BATCH_JOB_NAME).name());
    }
}
