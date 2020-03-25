package com.ihsmarkit.tfx.eod.controller;

import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.ihsmarkit.tfx.eod.api.EodServiceControllerApi;
import com.ihsmarkit.tfx.eod.exception.LockException;
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
    public ResponseEntity<String> runEOD2(@Valid final Boolean generateMonthlyLedger) {
        return ResponseEntity.ok(eodControlService.runEOD2Job(generateMonthlyLedger).name());
    }

    @Override
    public ResponseEntity<LocalDate> rollBusinessDate() {
        eodControlService.rollBusinessDateJob();
        return getCurrentBusinessDay();
    }

    @Override
    public ResponseEntity<LocalDate> runAll(@Valid final Boolean generateMonthlyLedger) {
        return ResponseEntity.ok(eodControlService.runAll(generateMonthlyLedger));
    }

    @Override
    public ResponseEntity<String> updateCashBalances() {
        return ResponseEntity.ok(eodControlService.runCashBalanceUpdateJob().name());
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "EOD service is busy with another task, please try again later")
    @ExceptionHandler(LockException.class)
    public void lockException() {

    }
}
