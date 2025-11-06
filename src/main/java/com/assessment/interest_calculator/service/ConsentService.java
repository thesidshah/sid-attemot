package com.assessment.interest_calculator.service;

import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.assessment.interest_calculator.config.DigioAAProperties;
import com.assessment.interest_calculator.repository.ConsentRequestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConsentService {
    private final DigioAAClient digioAAClient;
    private final ConsentRequestRepository consentRequestRepository;
    private final DigioAAProperties digioAAProperties;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");


}
