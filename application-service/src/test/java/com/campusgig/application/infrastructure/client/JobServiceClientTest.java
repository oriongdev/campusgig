package com.campusgig.application.infrastructure.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * The synchronous REST call from application-service to job-service, tested against a fake
 * job-service so the contract between the two is pinned down without starting either.
 */
@RestClientTest(JobServiceClient.class)
@TestPropertySource(properties = "campusgig.job-service.url=http://job-service.test")
class JobServiceClientTest {

    @Autowired
    private JobServiceClient client;

    @Autowired
    private MockRestServiceServer jobService;

    @Test
    void readsOnlyTheFieldsItNeedsFromJobServicesResponse() {
        jobService.expect(requestTo("http://job-service.test/api/jobs/1")).andRespond(withSuccess("""
                {"id":1,"employerId":7,"title":"Weekend Barista","description":"Make coffee.","category":"hospitality",
                 "location":"Wollongong","payAmount":28.50,"payType":"HOURLY","status":"OPEN","postedAt":"2026-10-07T04:59:43Z"}
                """, MediaType.APPLICATION_JSON));

        Optional<JobServiceClient.JobSummary> job = client.findJob(1L);

        assertTrue(job.isPresent());
        assertEquals("Weekend Barista", job.get().title());
        assertTrue(job.get().isOpen());
    }

    @Test
    void aClosedJobIsReportedAsNotOpen() {
        jobService.expect(requestTo("http://job-service.test/api/jobs/2")).andRespond(withSuccess(
                "{\"id\":2,\"title\":\"Java Tutor\",\"status\":\"CLOSED\"}", MediaType.APPLICATION_JSON));

        assertFalse(client.findJob(2L).orElseThrow().isOpen());
    }

    @Test
    void a404FromJobServiceMeansThereIsNoSuchJob() {
        jobService.expect(requestTo("http://job-service.test/api/jobs/99")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertTrue(client.findJob(99L).isEmpty());
    }
}
