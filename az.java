@RestController
@RequestMapping("/api/jobs")
public class JenkinsController {

    private final JenkinsIntegrationService jenkinsService;

    public JenkinsController(JenkinsIntegrationService jenkinsService) {
        this.jenkinsService = jenkinsService;
    }

    @PostMapping("/run")
    public CompletableFuture<ResponseEntity<JobReportDto>> runJob(@RequestParam String executionId) {
        return jenkinsService.triggerAndAwaitReport(executionId)
                .thenApply(ResponseEntity::ok);
    }
}


package com.example.jenkins.service;

import com.example.jenkins.dto.*;
        import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

@Service
public class JenkinsIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(JenkinsIntegrationService.class);

    private final RestClient jenkinsClient;

    // Konfiguracja klienta z Basic Auth dla Jenkinsa
    public JenkinsIntegrationService(RestClient.Builder builder) {
        this.jenkinsClient = builder
                .baseUrl("https://jenkins.your-domain.com")
                .defaultHeaders(headers -> headers.setBasicAuth("admin", "11aabbcc_API_TOKEN"))
                .build();
    }

    @Async
    public CompletableFuture<JobReportDto> triggerAndAwaitReport(String executionId) {
        try {
            // 1. TRIGGER JOBA
            ResponseEntity<Void> triggerResponse = jenkinsClient.post()
                    .uri("/job/MojaAutomatyzacja/buildWithParameters?EXTERNAL_EXECUTION_ID={id}", executionId)
                    .retrieve()
                    .toBodilessEntity();

            URI queueItemUri = triggerResponse.getHeaders().getLocation();
            if (queueItemUri == null) {
                throw new IllegalStateException("Jenkins nie zwrócił nagłówka Location dla kolejki!");
            }

            log.info("[{}] Job w kolejce: {}", executionId, queueItemUri);

            // 2. CZEKANIE NA WYSTARTOWANIE JOBA (Pobranie Build Number)
            int buildNumber = awaitBuildStart(queueItemUri);
            log.info("[{}] Job wystartował jako Build #{}", executionId, buildNumber);

            // 3. POLLING STANU ZAKOŃCZENIA JOBA
            String result = awaitJobCompletion("MojaAutomatyzacja", buildNumber);
            log.info("[{}] Job zakończony ze statusem: {}", executionId, result);

            if (!"SUCCESS".equals(result)) {
                throw new RuntimeException("Job zakończył się niepowodzeniem: " + result);
            }

            // 4. POBRANIE ARTEFAKTU Z RAPORTEM
            JobReportDto report = jenkinsClient.get()
                    .uri("/job/MojaAutomatyzacja/{buildNumber}/artifact/reports/result.json", buildNumber)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JobReportDto.class);

            return CompletableFuture.completedFuture(report);

        } catch (Exception e) {
            log.error("[{}] Błąd podczas wykonywania joba Jenkins", executionId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private int awaitBuildStart(URI queueItemUri) throws InterruptedException {
        while (true) {
            JenkinsQueueResponse queueRes = jenkinsClient.get()
                    .uri(queueItemUri + "api/json")
                    .retrieve()
                    .body(JenkinsQueueResponse.class);

            if (queueRes != null && queueRes.executable() != null) {
                return queueRes.executable().number();
            }
            Thread.sleep(2000); // Poll co 2s
        }
    }

    private String awaitJobCompletion(String jobName, int buildNumber) throws InterruptedException {
        while (true) {
            JenkinsBuildStatusResponse buildRes = jenkinsClient.get()
                    .uri("/job/{jobName}/{buildNumber}/api/json", jobName, buildNumber)
                    .retrieve()
                    .body(JenkinsBuildStatusResponse.class);

            if (buildRes != null && !buildRes.building()) {
                return buildRes.result();
            }
            Thread.sleep(5000); // Poll co 5s
        }
    }
}


// Reprezentacja odpowiedzi z kolejki (Queue item)
public record JenkinsQueueResponse(Executable executable) {
    public record Executable(int number, String url) {}
}

// Reprezentacja stanu builda
public record JenkinsBuildStatusResponse(boolean building, String result) {}

// Przykład Twojego raportu końcowego
public record JobReportDto(String executionId, String status, int coverage) {}