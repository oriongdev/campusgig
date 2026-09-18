package com.campusgig.application.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Synchronous REST call to job-service, used to check a job exists and is open
 * before an application is accepted.
 */
@Component
public class JobServiceClient {

    private final RestClient restClient;

    public JobServiceClient(RestClient.Builder builder,
                            @Value("${campusgig.job-service.url}") String jobServiceUrl) {
        this.restClient = builder.baseUrl(jobServiceUrl).build();
    }

    public Optional<JobSummary> findJob(Long jobId) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/api/jobs/{id}", jobId)
                    .retrieve()
                    .body(JobSummary.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    /** Only the fields this service needs from job-service's response. */
    public record JobSummary(Long id, String title, String status) {

        public boolean isOpen() {
            return "OPEN".equals(status);
        }
    }
}
