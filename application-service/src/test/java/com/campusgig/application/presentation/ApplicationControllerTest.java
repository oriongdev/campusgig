package com.campusgig.application.presentation;

import com.campusgig.application.domain.ApplicationStatus;
import com.campusgig.application.domain.JobApplication;
import com.campusgig.application.service.ApplicationService;
import com.campusgig.application.service.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicationController.class)
class ApplicationControllerTest {

    private static final String APPLY = """
            {"jobId":1,"studentId":2,"coverMessage":"I have two years of barista experience."}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApplicationService applicationService;

    private JobApplication application() {
        return new JobApplication(1L, 2L, "I have two years of barista experience.");
    }

    @Test
    void applyingReturns201WithASubmittedApplication() throws Exception {
        when(applicationService.submit(1L, 2L, "I have two years of barista experience.")).thenReturn(application());

        mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content(APPLY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobId").value(1))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void applyingTwiceReturns409() throws Exception {
        when(applicationService.submit(1L, 2L, "I have two years of barista experience."))
                .thenThrow(new IllegalStateException("Student 2 has already applied for job 1"));

        mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content(APPLY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Student 2 has already applied for job 1"));
    }

    @Test
    void applyingForAJobThatDoesNotExistReturns404() throws Exception {
        when(applicationService.submit(1L, 2L, "I have two years of barista experience."))
                .thenThrow(new NotFoundException("Job 1 not found"));

        mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content(APPLY))
                .andExpect(status().isNotFound());
    }

    @Test
    void ifJobServiceIsDownTheStudentGets503InsteadOfAStackTrace() throws Exception {
        when(applicationService.submit(1L, 2L, "I have two years of barista experience."))
                .thenThrow(new ResourceAccessException("Connection refused"));

        mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content(APPLY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("job-service is not reachable. Is it running on port 8082?"));
    }

    @Test
    void anEmployerListsApplicationsByJobAndAStudentByStudent() throws Exception {
        when(applicationService.forJob(1L)).thenReturn(List.of(application()));
        when(applicationService.forStudent(2L)).thenReturn(List.of(application(), application()));

        mockMvc.perform(get("/api/applications?jobId=1")).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/applications?studentId=2")).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void listingWithoutAFilterReturns400() throws Exception {
        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Provide a jobId or studentId query parameter"));
    }

    @Test
    void anEmployerChangesTheStatus() throws Exception {
        JobApplication shortlisted = application();
        shortlisted.moveTo(ApplicationStatus.SHORTLISTED);
        when(applicationService.changeStatus(5L, ApplicationStatus.SHORTLISTED)).thenReturn(shortlisted);

        mockMvc.perform(put("/api/applications/5/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHORTLISTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHORTLISTED"));
    }

    @Test
    void aStatusChangeThatBreaksTheRulesReturns409() throws Exception {
        when(applicationService.changeStatus(5L, ApplicationStatus.REJECTED))
                .thenThrow(new IllegalStateException("Cannot change application 5 from ACCEPTED to REJECTED"));

        mockMvc.perform(put("/api/applications/5/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void aStudentWithdrawsTheirApplication() throws Exception {
        JobApplication withdrawn = application();
        withdrawn.moveTo(ApplicationStatus.WITHDRAWN);
        when(applicationService.withdraw(5L)).thenReturn(withdrawn);

        mockMvc.perform(post("/api/applications/5/withdraw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"));
    }
}
