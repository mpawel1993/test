package com.example.demo.dto.playwright;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PlaywrightReportDto(
        StatsDto stats,
        List<SuiteDto> suites,
        List<ErrorDto> errors
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record StatsDto(
            long startTime,
            double duration,
            int expected,     // passed
            int unexpected,   // failed
            int skipped,
            int flaky
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SuiteDto(
            String title,
            String file,
            int line,
            List<SuiteDto> suites, // pod-zestawy (sub-suites)
            List<SpecDto> specs
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SpecDto(
            String id,
            String title,
            String file,
            int line,
            List<TestDto> tests
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record TestDto(
            String timeout,
            String status, // expected, unexpected, skipped, flaky
            List<TestResultDto> results
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record TestResultDto(
            int retry,
            String status, // passed, failed, timedOut, skipped
            double duration,
            ErrorDto error,
            List<ErrorDto> errors,
            List<AttachmentDto> attachments
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ErrorDto(
            String message,
            String stack,
            String value
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AttachmentDto(
            String name,
            String contentType,
            String path
    ) {}
}


package com.example.demo.service;

import com.example.demo.dto.playwright.PlaywrightReportDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
public class PlaywrightReportService {

    private final ObjectMapper objectMapper;

    // Spring automatycznie wstrzyknie skonfigurowany ObjectMapper
    public PlaywrightReportService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // Odczyt z pliku na dysku
    public PlaywrightReportDto parseReportFromFile(String filePath) throws IOException {
        return objectMapper.readValue(new File(filePath), PlaywrightReportDto.class);
    }

    // Odczyt z pliku przesłanego przez REST API (MultipartFile)
    public PlaywrightReportDto parseReportFromUpload(MultipartFile file) throws IOException {
        return objectMapper.readValue(file.getInputStream(), PlaywrightReportDto.class);
    }
}


package com.example.demo.controller;

import com.example.demo.dto.playwright.PlaywrightReportDto;
import com.example.demo.service.PlaywrightReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
        import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/reports")
public class PlaywrightReportController {

    private final PlaywrightReportService reportService;

    public PlaywrightReportController(PlaywrightReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadReport(@RequestParam("file") MultipartFile file) throws IOException {
        PlaywrightReportDto report = reportService.parseReportFromUpload(file);

        int totalFailed = report.stats().unexpected();
        int totalPassed = report.stats().expected();

        return ResponseEntity.ok(
                String.format("Raport przetworzony. Sukcesy: %d, Błędy: %d", totalPassed, totalFailed)
        );
    }
}