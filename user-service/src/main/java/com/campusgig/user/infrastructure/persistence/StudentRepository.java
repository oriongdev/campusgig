package com.campusgig.user.infrastructure.persistence;

import com.campusgig.user.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByEmailValue(String email);
}
