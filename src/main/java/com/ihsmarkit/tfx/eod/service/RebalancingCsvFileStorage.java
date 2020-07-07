package com.ihsmarkit.tfx.eod.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.common.files.SimpleFileStorage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RebalancingCsvFileStorage {

    private static final DateTimeFormatter BUSINESS_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String CSV_FILE_EXTENSION = ".csv";

    private final SimpleFileStorage fileStorage;

    RebalancingCsvFileStorage(@Value("${data.rebalancing-csv.dir}") final String basePath) {
        this.fileStorage = new SimpleFileStorage(basePath);
    }

    public void store(final LocalDate businessDate, final Map<String, byte[]> files) {
        log.info("Storing rebalancing csv for business date: {} and for participant(s): {}", businessDate, files.keySet());

        final String folderName = BUSINESS_DATE_FORMAT.format(businessDate);
        files.forEach((participantCode, file) -> fileStorage.storeFile(folderName, participantCode + CSV_FILE_EXTENSION, file));
    }

}
