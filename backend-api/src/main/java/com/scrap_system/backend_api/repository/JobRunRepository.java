package com.scrap_system.backend_api.repository;

import com.scrap_system.backend_api.model.JobRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRunRepository extends JpaRepository<JobRun, Long> {
    Page<JobRun> findByJobTypeOrderByStartedAtDesc(String jobType, Pageable pageable);
}

