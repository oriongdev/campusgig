package com.campusgig.job.presentation;

import com.campusgig.job.presentation.dto.JobResponse;
import com.campusgig.job.presentation.dto.PostJobRequest;
import com.campusgig.job.service.JobService;
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

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobResponse post(@RequestBody PostJobRequest request) {
        return JobResponse.from(jobService.postJob(
                request.employerId(), request.title(), request.description(), request.category(),
                request.location(), request.payAmount(), request.payType()));
    }

    /** Employers can edit an open job (requirement 5). */
    @PutMapping("/{id}")
    public JobResponse update(@PathVariable("id") Long id, @RequestBody PostJobRequest request) {
        return JobResponse.from(jobService.updateJob(
                id, request.title(), request.description(), request.category(),
                request.location(), request.payAmount(), request.payType()));
    }

    @GetMapping("/{id}")
    public JobResponse get(@PathVariable("id") Long id) {
        return JobResponse.from(jobService.getJob(id));
    }

    /** Search open jobs, e.g. /api/jobs?category=hospitality&location=wollongong&minPay=25 */
    @GetMapping
    public List<JobResponse> search(@RequestParam(name = "category", required = false) String category,
                                    @RequestParam(name = "location", required = false) String location,
                                    @RequestParam(name = "minPay", required = false) BigDecimal minPay) {
        return jobService.searchOpenJobs(category, location, minPay).stream().map(JobResponse::from).toList();
    }

    @GetMapping("/employer/{employerId}")
    public List<JobResponse> forEmployer(@PathVariable("employerId") Long employerId) {
        return jobService.jobsForEmployer(employerId).stream().map(JobResponse::from).toList();
    }

    @PostMapping("/{id}/close")
    public JobResponse close(@PathVariable("id") Long id) {
        return JobResponse.from(jobService.closeJob(id));
    }
}
