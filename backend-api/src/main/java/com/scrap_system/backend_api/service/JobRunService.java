package com.scrap_system.backend_api.service;

import com.scrap_system.backend_api.model.JobRun;
import com.scrap_system.backend_api.repository.JobRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobRunService {

    private final JobRunRepository jobRunRepository;

    @Transactional
    public JobRun start(String jobType, String runId, Long actorUserId, String actorName, String detailsJson) {
        JobRun jr = new JobRun();
        jr.setRunId(runId != null && !runId.isBlank() ? runId.trim() : UUID.randomUUID().toString());
        jr.setJobType(jobType);
        jr.setStatus("RUNNING");
        jr.setStartedAt(LocalDateTime.now());
        jr.setActorUserId(actorUserId);
        jr.setActorName(actorName);
        jr.setDetailsJson(detailsJson);
        return jobRunRepository.save(jr);
    }

    @Transactional
    public JobRun createPending(String jobType, String runId, Long actorUserId, String actorName, String detailsJson) {
        JobRun jr = new JobRun();
        jr.setRunId(runId != null && !runId.isBlank() ? runId.trim() : UUID.randomUUID().toString());
        jr.setJobType(jobType);
        jr.setStatus("PENDING");
        jr.setStartedAt(LocalDateTime.now());
        jr.setActorUserId(actorUserId);
        jr.setActorName(actorName);
        jr.setDetailsJson(detailsJson);
        return jobRunRepository.save(jr);
    }

    @Transactional
    public JobRun markRunning(JobRun jr, String message, String detailsJson) {
        jr.setStatus("RUNNING");
        jr.setMessage(message);
        if (detailsJson != null) jr.setDetailsJson(detailsJson);
        return jobRunRepository.save(jr);
    }

    @Transactional
    public JobRun updateProgress(JobRun jr, Integer inserted, Integer updated, Integer skipped, String message, String detailsJson) {
        if (inserted != null) jr.setInsertedCount(inserted);
        if (updated != null) jr.setUpdatedCount(updated);
        if (skipped != null) jr.setSkippedCount(skipped);
        if (message != null) jr.setMessage(message);
        if (detailsJson != null) jr.setDetailsJson(detailsJson);
        return jobRunRepository.save(jr);
    }

    @Transactional
    public JobRun success(JobRun jr, Integer inserted, Integer updated, Integer skipped, String message, String detailsJson) {
        jr.setStatus("SUCCESS");
        jr.setFinishedAt(LocalDateTime.now());
        jr.setInsertedCount(inserted);
        jr.setUpdatedCount(updated);
        jr.setSkippedCount(skipped);
        jr.setMessage(message);
        if (detailsJson != null) jr.setDetailsJson(detailsJson);
        return jobRunRepository.save(jr);
    }

    @Transactional
    public JobRun failed(JobRun jr, String message, String detailsJson) {
        jr.setStatus("FAILED");
        jr.setFinishedAt(LocalDateTime.now());
        jr.setMessage(message);
        if (detailsJson != null) jr.setDetailsJson(detailsJson);
        return jobRunRepository.save(jr);
    }
}
