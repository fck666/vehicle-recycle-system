package com.scrap_system.backend_api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(
        name = "job_run",
        indexes = {
                @Index(name = "idx_job_run_type_time", columnList = "job_type, started_at"),
                @Index(name = "idx_job_run_run_id", columnList = "run_id", unique = true)
        }
)
public class JobRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", length = 64, nullable = false, unique = true)
    private String runId;

    @Column(name = "job_type", length = 64, nullable = false)
    private String jobType;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_name", length = 64)
    private String actorName;

    @Column(name = "inserted_count")
    private Integer insertedCount;

    @Column(name = "updated_count")
    private Integer updatedCount;

    @Column(name = "skipped_count")
    private Integer skippedCount;

    @Column(name = "message", length = 512)
    private String message;

    @Lob
    @Column(name = "details_json", columnDefinition = "LONGTEXT")
    private String detailsJson;
}

