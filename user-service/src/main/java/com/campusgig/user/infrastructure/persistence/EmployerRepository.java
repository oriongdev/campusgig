package com.campusgig.user.infrastructure.persistence;

import com.campusgig.user.domain.Employer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployerRepository extends JpaRepository<Employer, Long> {

    boolean existsByEmailValue(String email);
}
