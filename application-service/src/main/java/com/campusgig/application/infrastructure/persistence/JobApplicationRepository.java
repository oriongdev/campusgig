package com.campusgig.application.infrastructure.persistence;

import com.campusgig.application.domain.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    boolean existsByJobIdAndStudentId(Long jobId, Long studentId);

    List<JobApplication> findByJobId(Long jobId);

    List<JobApplication> findByStudentId(Long studentId);
}
