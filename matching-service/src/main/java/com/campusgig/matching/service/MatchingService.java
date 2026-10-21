package com.campusgig.matching.service;

import com.campusgig.matching.domain.JobRecommendation;
import com.campusgig.matching.domain.JobSnapshot;
import com.campusgig.matching.domain.SkillMatcher;
import com.campusgig.matching.domain.StudentSnapshot;
import com.campusgig.matching.domain.event.JobClosed;
import com.campusgig.matching.domain.event.JobPosted;
import com.campusgig.matching.domain.event.JobUpdated;
import com.campusgig.matching.domain.event.StudentProfileUpdated;
import com.campusgig.matching.infrastructure.persistence.JobSnapshotRepository;
import com.campusgig.matching.infrastructure.persistence.StudentSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MatchingService {

    private final JobSnapshotRepository jobSnapshots;
    private final StudentSnapshotRepository studentSnapshots;
    private final SkillMatcher skillMatcher = new SkillMatcher();

    public MatchingService(JobSnapshotRepository jobSnapshots, StudentSnapshotRepository studentSnapshots) {
        this.jobSnapshots = jobSnapshots;
        this.studentSnapshots = studentSnapshots;
    }

    public void recordJob(JobPosted event) {
        jobSnapshots.save(new JobSnapshot(
                event.jobId(), event.title(), event.description(), event.category(), event.location()));
    }

    public void updateJob(JobUpdated event) {
        jobSnapshots.save(new JobSnapshot(
                event.jobId(), event.title(), event.description(), event.category(), event.location()));
    }

    public void removeJob(JobClosed event) {
        jobSnapshots.deleteById(event.jobId());
    }

    public void recordStudent(StudentProfileUpdated event) {
        StudentSnapshot student = studentSnapshots.findById(event.studentId())
                .orElseGet(() -> new StudentSnapshot(event.studentId()));
        student.update(event.name(), event.skills(), event.availability());
        studentSnapshots.save(student);
    }

    /**
     * Rule-based recommendations: every open job whose text mentions one of the student's skills.
     * This is the instant, deterministic baseline. The AI matching agent (see AiAgentService)
     * gives richer answers and falls back to this when the LLM is unavailable.
     */
    @Transactional(readOnly = true)
    public List<JobRecommendation> recommendJobs(Long studentId) {
        StudentSnapshot student = studentSnapshots.findById(studentId)
                .orElseThrow(() -> new NotFoundException("No profile received yet for student " + studentId));
        return jobSnapshots.findAll().stream()
                .map(job -> skillMatcher.match(student, job))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingDouble(JobRecommendation::score).reversed())
                .toList();
    }
}
