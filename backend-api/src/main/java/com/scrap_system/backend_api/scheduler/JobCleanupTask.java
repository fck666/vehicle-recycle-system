package com.scrap_system.backend_api.scheduler;

import com.scrap_system.backend_api.model.JobRun;
import com.scrap_system.backend_api.repository.JobRunRepository;
import com.scrap_system.backend_api.service.JobRunService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobCleanupTask {

    private final JobRunRepository jobRunRepository;
    private final JobRunService jobRunService;

    /**
     * 每小时检查一次，自动关闭超过 24 小时未完成的任务
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupTimeoutJobs() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        
        // Find RUNNING jobs started before 24h ago
        List<JobRun> runningJobs = jobRunRepository.findByStatusAndStartedAtBefore("RUNNING", threshold);
        for (JobRun job : runningJobs) {
            log.info("Marking timeout job as FAILED: runId={}, startedAt={}", job.getRunId(), job.getStartedAt());
            jobRunService.failed(job, "timeout: job exceeded 24h execution time", null);
        }

        // Find PENDING jobs started before 24h ago (stale pending)
        List<JobRun> pendingJobs = jobRunRepository.findByStatusAndStartedAtBefore("PENDING", threshold);
        for (JobRun job : pendingJobs) {
             log.info("Marking stale pending job as FAILED: runId={}, startedAt={}", job.getRunId(), job.getStartedAt());
             jobRunService.failed(job, "timeout: job pending for >24h without claim", null);
        }
    }
}
