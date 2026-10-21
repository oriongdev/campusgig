package com.campusgig.job.presentation;

import com.campusgig.job.domain.Job;
import com.campusgig.job.domain.Pay;
import com.campusgig.job.domain.PayType;
import com.campusgig.job.service.JobService;
import com.campusgig.job.service.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobController.class)
class JobControllerTest {

    private static final String BARISTA_JOB = """
            {"employerId":1,"title":"Weekend Barista","description":"Make coffee.","category":"hospitality",
             "location":"Wollongong","payAmount":28.50,"payType":"HOURLY"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobService jobService;

    private Job barista() {
        return new Job(1L, "Weekend Barista", "Make coffee.", "hospitality", "Wollongong",
                new Pay(new BigDecimal("28.50"), PayType.HOURLY));
    }

    @Test
    void postingAJobReturns201WithTheOpenJob() throws Exception {
        when(jobService.postJob(eq(1L), eq("Weekend Barista"), any(), eq("hospitality"), eq("Wollongong"),
                eq(new BigDecimal("28.50")), eq(PayType.HOURLY))).thenReturn(barista());

        mockMvc.perform(post("/api/jobs").contentType(MediaType.APPLICATION_JSON).content(BARISTA_JOB))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Weekend Barista"))
                .andExpect(jsonPath("$.payAmount").value(28.50))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void aJobThatBreaksADomainRuleReturns400WithTheReason() throws Exception {
        when(jobService.postJob(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Pay amount must be greater than zero"));

        mockMvc.perform(post("/api/jobs").contentType(MediaType.APPLICATION_JSON).content(BARISTA_JOB))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Pay amount must be greater than zero"));
    }

    @Test
    void anUnknownJobReturns404() throws Exception {
        when(jobService.getJob(99L)).thenThrow(new NotFoundException("Job 99 not found"));

        mockMvc.perform(get("/api/jobs/99")).andExpect(status().isNotFound());
    }

    @Test
    void searchPassesTheFiltersToTheService() throws Exception {
        when(jobService.searchOpenJobs("hospitality", "wollongong", new BigDecimal("25")))
                .thenReturn(List.of(barista()));

        mockMvc.perform(get("/api/jobs?category=hospitality&location=wollongong&minPay=25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].category").value("hospitality"));
    }

    @Test
    void searchWorksWithNoFiltersAtAll() throws Exception {
        when(jobService.searchOpenJobs(null, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/jobs")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void editingAClosedJobReturns409() throws Exception {
        when(jobService.updateJob(eq(1L), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("Job 1 is closed and cannot be edited"));

        mockMvc.perform(put("/api/jobs/1").contentType(MediaType.APPLICATION_JSON).content(BARISTA_JOB))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Job 1 is closed and cannot be edited"));
    }

    @Test
    void closingAJobReturnsTheClosedJob() throws Exception {
        Job closed = barista();
        closed.close();
        when(jobService.closeJob(1L)).thenReturn(closed);

        mockMvc.perform(post("/api/jobs/1/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
        verify(jobService).closeJob(1L);
    }

    @Test
    void anEmployerCanListTheirOwnJobs() throws Exception {
        when(jobService.jobsForEmployer(1L)).thenReturn(List.of(barista()));

        mockMvc.perform(get("/api/jobs/employer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employerId").value(1));
    }
}
