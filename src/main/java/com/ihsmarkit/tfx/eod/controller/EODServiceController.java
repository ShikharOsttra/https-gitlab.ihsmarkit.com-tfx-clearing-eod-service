package com.ihsmarkit.tfx.eod.controller;

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
    public ResponseEntity<LocalDate> undoLatestEOD() {
        return ResponseEntity.ok(eodControlService.undoLatestEODResults());
    }
}
