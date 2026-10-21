package com.campusgig.matching.presentation;

import com.campusgig.matching.domain.TrendingJob;
import com.campusgig.matching.service.AnalyticsQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The JSON shape of the real-time analysis endpoints (requirements 11 and 12). */
@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsQueryService analytics;

    @Test
    void countsAreReturnedAsJsonObjectsKeyedByCategoryAndByJobId() throws Exception {
        when(analytics.jobsPerCategory()).thenReturn(Map.of("hospitality", 2L, "tutoring", 1L));
        when(analytics.applicationsPerJob()).thenReturn(Map.of(1L, 3L));

        mockMvc.perform(get("/api/analytics/jobs-per-category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hospitality").value(2))
                .andExpect(jsonPath("$.tutoring").value(1));
        mockMvc.perform(get("/api/analytics/applications-per-job"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.1").value(3));
    }

    @Test
    void trendingJobsAndTheSourceOfTheNumbersAreReported() throws Exception {
        when(analytics.trendingJobs()).thenReturn(List.of(new TrendingJob(1L, "Weekend Barista", 3)));
        when(analytics.source()).thenReturn(AnalyticsQueryService.SOURCE_STREAMS);

        mockMvc.perform(get("/api/analytics/trending-jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobId").value(1))
                .andExpect(jsonPath("$[0].title").value("Weekend Barista"))
                .andExpect(jsonPath("$[0].applicationsInWindow").value(3));
        mockMvc.perform(get("/api/analytics/source"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("kafka streams"));
    }
}
