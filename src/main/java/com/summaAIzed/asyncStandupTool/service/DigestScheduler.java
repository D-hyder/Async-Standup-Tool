package com.summaAIzed.asyncStandupTool.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DigestScheduler {

    private final StandupService standupService;

    public DigestScheduler(StandupService standupService) {
        this.standupService = standupService;
    }

    // 5:00 PM server time, Mon–Fri
    @Scheduled(cron = "0 0 17 * * MON-FRI")
    public void runDailyDigest() throws JsonProcessingException {
        standupService.postDailyDigest(LocalDate.now());
    }
}
