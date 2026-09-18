package com.campusgig.matching.infrastructure.persistence;

import com.campusgig.matching.domain.StudentSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentSnapshotRepository extends JpaRepository<StudentSnapshot, Long> {
}
