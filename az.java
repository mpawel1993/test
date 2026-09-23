package com.example.jenkins.service;

import com.example.jenkins.dto.JobReportDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class JenkinsJobService {

    private final RestClient jenkinsClient;
    private final String remoteToken;
    private final String myCallbackBaseUrl;

    // Mapa przechowująca obietnice wyników (Future) dla aktywnych zadań
    private final Map<String, CompletableFuture<JobReportDto>> pendingJobs = new ConcurrentHashMap<>();

    public JenkinsJobService(
            RestClient.Builder builder,
            @Value("${jenkins.url}") String jenkinsUrl,
            @Value("${jenkins.remote-token}") String remoteToken,
            @Value("${app.callback-base-url}") String myCallbackBaseUrl) {

        this.jenkinsClient = builder.baseUrl(jenkinsUrl).build();
        this.remoteToken = remoteToken;
        this.myCallbackBaseUrl = myCallbackBaseUrl;
    }

    /**
     * Startuje job w Jenkinsie i czeka na sygnał (Callback) z limitem czasu.
     */
    public JobReportDto triggerAndAwaitReport(String jobName, String executionId) {
        log.info("[{}] Inicjalizacja joba: {}", executionId, jobName);

        // 1. Rejestrujemy obietnicę wyniku
        CompletableFuture<JobReportDto> future = new CompletableFuture<>();
        pendingJobs.put(executionId, future);

        try {
            // Adres, na który Jenkins wyśle POST po zakończeniu
            String callbackUrl = myCallbackBaseUrl + "/api/jenkins/callback";

            // 2. Strzał do Jenkinsa z tokenem i parametrami
            jenkinsClient.post()
                    .uri("/job/{jobName}/buildWithParameters?token={token}&EXTERNAL_EXECUTION_ID={id}&CALLBACK_URL={callback}",
                            jobName, remoteToken, executionId, callbackUrl)
                    .retrieve()
                    .toBodilessEntity();

            log.info("[{}] Job wysłany. Czekam na callback...", executionId);

            // 3. Czekamy na sygnał z Jenkinsa (np. maksymalnie 15 minut)
            return future.get(15, TimeUnit.MINUTES);

        } catch (Exception e) {
            log.error("[{}] Błąd lub timeout podczas oczekiwania na job", executionId, e);
            throw new RuntimeException("Nie udało się pobrać raportu dla executionId: " + executionId, e);
        } finally {
            // Sprzątamy mapę po zakończeniu / błędzie
            pendingJobs.remove(executionId);
        }
    }

    /**
     * Wywoływane przez Controller, gdy Jenkins przysyła sygnał.
     */
    public void processCallback(JobReportDto payload) {
        log.info("[{}] Otrzymano callback z Jenkinsa! Status: {}", payload.executionId(), payload.result());

        CompletableFuture<JobReportDto> future = pendingJobs.get(payload.executionId());
        if (future != null) {
            future.complete(payload); // Pobudza wątek czekający w triggerAndAwaitReport
        } else {
            log.warn("[{}] Otrzymano callback dla nieznanego lub przedawnionego joba!", payload.executionId());
        }
    }
}

package com.example.jenkins.controller;

import com.example.jenkins.dto.JobReportDto;
import com.example.jenkins.service.JenkinsJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jenkins")
@RequiredArgsConstructor
public class JenkinsController {

    private final JenkinsJobService jenkinsJobService;

    // Endpoint wywoływany z Twojego UI
    @PostMapping("/run")
    public ResponseEntity<JobReportDto> runJob(
            @RequestParam String jobName,
            @RequestParam String executionId) {

        JobReportDto report = jenkinsJobService.triggerAndAwaitReport(jobName, executionId);
        return ResponseEntity.ok(report);
    }

    // Endpoint wywoływany przez Jenkinsa (Sygnał Callback)
    @PostMapping("/callback")
    public ResponseEntity<Void> handleCallback(@RequestBody JobReportDto payload) {
        jenkinsJobService.processCallback(payload);
        return ResponseEntity.ok().build();
    }
}

package com.example.jenkins.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Raport przekazywany z Jenkinsa w callbacku
@JsonIgnoreProperties(ignoreUnknown = true)
public record JobReportDto(
        String executionId,
        int buildNumber,
        String result,     // SUCCESS, FAILURE, ABORTED
        Object details     // Dowolna treść raportu wygenerowana przez job
) {}