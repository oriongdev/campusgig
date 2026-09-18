package com.campusgig.job.infrastructure.persistence;

import com.campusgig.job.domain.Job;
import com.campusgig.job.domain.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByStatus(JobStatus status);

    List<Job> findByEmployerId(Long employerId);
}
