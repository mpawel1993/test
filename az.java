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
            int expected,
            int unexpected,
            int skipped,
            int flaky
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SuiteDto(
            String title,
            String file,
            int line,
            List<SuiteDto> suites,
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
            String status,
            List<TestResultDto> results
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record TestResultDto(
            int retry,
            String status,
            double duration,
            ErrorDto error,
            List<ErrorDto> errors,
            List<AttachmentDto> attachments,
            List<StepDto> steps // <--- DODANA LISTA KROKÓW
    ) {}

    // NOWY REKORD DLA KROKU TESTU
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record StepDto(
            String title,
            double duration,
            ErrorDto error,
            List<StepDto> steps, // <--- REKURENCJA: krok może mieć własne sub-stepy
            LocationDto location
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record LocationDto(
            String file,
            int line,
            int column
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