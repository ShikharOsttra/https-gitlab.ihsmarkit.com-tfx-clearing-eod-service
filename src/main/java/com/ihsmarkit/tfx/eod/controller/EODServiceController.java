package com.ihsmarkit.tfx.eod.controller;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD1_BATCH_JOB_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.EOD2_BATCH_JOB_NAME;

import java.time.LocalDate;

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
    public ResponseEntity<LocalDate> undoCurrentEOD() {
        return ResponseEntity.ok(eodControlService.undoCurrentDayEOD());
    }

    @Override
    public ResponseEntity<LocalDate> undoPreviousEOD() {
        return ResponseEntity.ok(eodControlService.undoPreviousDayEOD());
    }

    @Override
    public ResponseEntity<LocalDate> getCurrentBusinessDay() {
        return ResponseEntity.ok(eodControlService.getCurrentBusinessDate());
    }

    @Override
    public ResponseEntity<LocalDate> runEOD1() {
        return ResponseEntity.ok(eodControlService.runEOD(EOD1_BATCH_JOB_NAME));
    }

    @Override
    public ResponseEntity<LocalDate> runEOD2() {
        return ResponseEntity.ok(eodControlService.runEOD(EOD2_BATCH_JOB_NAME));
    }
}
