package com.campusgig.matching.infrastructure.persistence;

import com.campusgig.matching.domain.JobSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobSnapshotRepository extends JpaRepository<JobSnapshot, Long> {
}
