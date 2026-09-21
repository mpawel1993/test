package com.example.demo.dto.playwright;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PlaywrightReportDto(
        List<SuiteDto> suites
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SuiteDto(
            String title,
            List<SuiteDto> suites,
            List<SpecDto> specs
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SpecDto(
            String title,
            List<TestDto> tests
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record TestDto(
            String status, // ogólny status testu: expected, unexpected, skipped
            List<TestResultDto> results
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record TestResultDto(
            String status, // status próby: passed, failed, timedOut, skipped
            ErrorDto error,
            List<StepDto> steps
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record StepDto(
            String title,
            ErrorDto error,
            List<StepDto> steps
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ErrorDto(
            String message
    ) {}
}


package com.example.demo.dto.summary;

import java.util.List;

public record TestSummary(
        String testTitle,
        String testStatus, // passed / failed
        String testErrorMessage,
        List<StepSummary> steps
) {
    public record StepSummary(
            String stepTitle,
            String stepStatus, // passed / failed
            String stepErrorMessage
    ) {}
}

package com.example.demo.service;

import com.example.demo.dto.playwright.PlaywrightReportDto;
import com.example.demo.dto.summary.TestSummary;
import com.example.demo.dto.summary.TestSummary.StepSummary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PlaywrightReportParserService {

    public List<TestSummary> parseReport(PlaywrightReportDto report) {
        List<TestSummary> summaries = new ArrayList<>();
        if (report.suites() != null) {
            for (PlaywrightReportDto.SuiteDto suite : report.suites()) {
                extractTestsFromSuite(suite, summaries);
            }
        }
        return summaries;
    }

    private void extractTestsFromSuite(PlaywrightReportDto.SuiteDto suite, List<TestSummary> summaries) {
        if (suite.specs() != null) {
            for (PlaywrightReportDto.SpecDto spec : suite.specs()) {
                String testTitle = spec.title();

                for (PlaywrightReportDto.TestDto test : spec.tests()) {
                    for (PlaywrightReportDto.TestResultDto result : test.results()) {

                        String testStatus = result.status(); // passed, failed, timedOut
                        String testError = (result.error() != null) ? result.error().message() : null;

                        // Wyciąganie i spłaszczanie kroków
                        List<StepSummary> stepSummaries = new ArrayList<>();
                        if (result.steps() != null) {
                            for (PlaywrightReportDto.StepDto step : result.steps()) {
                                extractSteps(step, stepSummaries);
                            }
                        }

                        summaries.add(new TestSummary(testTitle, testStatus, testError, stepSummaries));
                    }
                }
            }
        }

        // Reagowanie na zagnieżdżone zestawy (sub-suites)
        if (suite.suites() != null) {
            for (PlaywrightReportDto.SuiteDto subSuite : suite.suites()) {
                extractTestsFromSuite(subSuite, summaries);
            }
        }
    }

    private void extractSteps(PlaywrightReportDto.StepDto step, List<StepSummary> stepSummaries) {
        // Określenie statusu kroku: jeśli jest obiekt error -> failed, w przeciwnym razie -> passed
        boolean hasError = step.error() != null;
        String stepStatus = hasError ? "failed" : "passed";
        String stepError = hasError ? step.error().message() : null;

        stepSummaries.add(new StepSummary(step.title(), stepStatus, stepError));

        // Jeśli krok posiada pod-kroki, przeliczamy je również
        if (step.steps() != null) {
            for (PlaywrightReportDto.StepDto subStep : step.steps()) {
                extractSteps(subStep, stepSummaries);
            }
        }
    }
}

package com.example.demo.controller;

import com.example.demo.dto.playwright.PlaywrightReportDto;
import com.example.demo.dto.summary.TestSummary;
import com.example.demo.service.PlaywrightReportParserService;
import org.springframework.web.bind.annotation.*;

        import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class PlaywrightReportController {

    private final PlaywrightReportParserService parserService;

    public PlaywrightReportController(PlaywrightReportParserService parserService) {
        this.parserService = parserService;
    }

    @PostMapping(value = "/summary", consumes = "application/json")
    public List<TestSummary> getReportSummary(@RequestBody PlaywrightReportDto rawReport) {
        return parserService.parseReport(rawReport);
    }
}