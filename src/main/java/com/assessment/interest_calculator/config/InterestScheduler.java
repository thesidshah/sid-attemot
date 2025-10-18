package com.assessment.interest_calculator.config;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.assessment.interest_calculator.service.InterestService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class InterestScheduler {

    private final InterestService interestService;

    @Value("${app.interest.zone:Asia/Kolkata}")
    private String zone;

    /**
     * Daily job that calculates and applies daily interest to all eligible loan accounts.
     * Runs every day at 11:59:00 PM IST (Indian Standard Time).
     *
     * Cron expression: "0 59 23 * * *" - runs at 23:59:00 (11:59 PM)
     * Time zone: Asia/Kolkata (IST)
     */
    @Scheduled(cron = "0 59 23 * * *", zone = "Asia/Kolkata")
    public void runDailyInterestCalculation() {
        LocalDate today = LocalDate.now(ZoneId.of(zone));
        log.info("Starting scheduled daily interest calculation for date: {}", today);

        try {
            interestService.applyDailyInterest(today);
            log.info("Successfully completed daily interest calculation for date: {}", today);
        } catch (Exception e) {
            log.error("Error during scheduled daily interest calculation for date: {}", today, e);
        }
    }

    /**
     * Monthly job that applies accrued interest to principal at month-end.
     * Runs on the last day of every month at 11:59:00 PM IST.
     *
     * Cron expression: "0 59 23 L * *" - runs at 23:59:00 on the last day of the month
     * Time zone: Asia/Kolkata (IST)
     *
     * This job implements monthly compounding by adding accumulated interest to principal.
     */
    @Scheduled(cron = "0 59 23 L * *", zone = "Asia/Kolkata")
    public void runMonthEndInterestApplication() {
        LocalDate today = LocalDate.now(ZoneId.of(zone));
        log.info("Starting scheduled month-end interest application for date: {}", today);

        try {
            interestService.applyMonthEndInterest(today);
            log.info("Successfully completed month-end interest application for date: {}", today);
        } catch (Exception e) {
            log.error("Error during scheduled month-end interest application for date: {}", today, e);
        }
    }
}
