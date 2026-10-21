package com.campusgig.job.infrastructure.persistence;

import com.campusgig.job.domain.Job;
import com.campusgig.job.domain.JobStatus;
import com.campusgig.job.domain.Pay;
import com.campusgig.job.domain.PayType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class JobRepositoryTest {

    @Autowired
    private JobRepository jobs;

    @Autowired
    private TestEntityManager entityManager;

    private Job job(Long employerId, String title, String amount, PayType type) {
        return new Job(employerId, title, "", "hospitality", "Wollongong", new Pay(new BigDecimal(amount), type));
    }

    @Test
    void findsOnlyOpenJobs() {
        jobs.save(job(1L, "Barista", "28.50", PayType.HOURLY));
        Job closed = job(1L, "Waiter", "27.00", PayType.HOURLY);
        closed.close();
        jobs.save(closed);

        List<Job> open = jobs.findByStatus(JobStatus.OPEN);

        assertEquals(1, open.size());
        assertEquals("Barista", open.get(0).getTitle());
    }

    @Test
    void findsJobsByEmployer() {
        jobs.save(job(1L, "Barista", "28.50", PayType.HOURLY));
        jobs.save(job(1L, "Waiter", "27.00", PayType.HOURLY));
        jobs.save(job(2L, "Event crew", "200", PayType.FIXED));

        assertEquals(2, jobs.findByEmployerId(1L).size());
        assertEquals(1, jobs.findByEmployerId(2L).size());
    }

    @Test
    void thePayValueObjectIsStoredInTheJobRowAndReadBack() {
        Long id = jobs.save(job(2L, "Event crew", "200.00", PayType.FIXED)).getId();
        entityManager.flush();
        entityManager.clear();

        Pay pay = jobs.findById(id).orElseThrow().getPay();

        assertEquals(0, new BigDecimal("200").compareTo(pay.amount()));
        assertEquals(PayType.FIXED, pay.type());
    }
}
