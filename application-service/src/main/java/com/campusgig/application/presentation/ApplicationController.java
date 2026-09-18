package com.campusgig.application.presentation;

import com.campusgig.application.presentation.dto.ApplicationResponse;
import com.campusgig.application.presentation.dto.ChangeStatusRequest;
import com.campusgig.application.presentation.dto.SubmitApplicationRequest;
import com.campusgig.application.service.ApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse submit(@RequestBody SubmitApplicationRequest request) {
        return ApplicationResponse.from(
                applicationService.submit(request.jobId(), request.studentId(), request.coverMessage()));
    }

    @GetMapping("/{id}")
    public ApplicationResponse get(@PathVariable("id") Long id) {
        return ApplicationResponse.from(applicationService.get(id));
    }

    /** List by job (employer view) or by student (student view): ?jobId=1 or ?studentId=1 */
    @GetMapping
    public List<ApplicationResponse> list(@RequestParam(name = "jobId", required = false) Long jobId,
                                          @RequestParam(name = "studentId", required = false) Long studentId) {
        if (jobId != null) {
            return applicationService.forJob(jobId).stream().map(ApplicationResponse::from).toList();
        }
        if (studentId != null) {
            return applicationService.forStudent(studentId).stream().map(ApplicationResponse::from).toList();
        }
        throw new IllegalArgumentException("Provide a jobId or studentId query parameter");
    }

    /** Employer shortlists, accepts or rejects an application. */
    @PutMapping("/{id}/status")
    public ApplicationResponse changeStatus(@PathVariable("id") Long id, @RequestBody ChangeStatusRequest request) {
        return ApplicationResponse.from(applicationService.changeStatus(id, request.status()));
    }

    @PostMapping("/{id}/withdraw")
    public ApplicationResponse withdraw(@PathVariable("id") Long id) {
        return ApplicationResponse.from(applicationService.withdraw(id));
    }
}
