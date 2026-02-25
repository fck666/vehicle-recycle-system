package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.model.JobRun;
import com.scrap_system.backend_api.repository.JobRunRepository;
import com.scrap_system.backend_api.service.JobRunService;
import com.scrap_system.backend_api.service.MaterialPriceFetchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/job-runs")
@RequiredArgsConstructor
public class AdminJobRunController {

    private final JobRunRepository jobRunRepository;
    private final JobRunService jobRunService;
    private final MaterialPriceFetchService materialPriceFetchService;

    @GetMapping
    public ResponseEntity<Page<JobRun>> list(
            @RequestParam String jobType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "startedAt"));
        return ResponseEntity.ok(jobRunRepository.findByJobTypeOrderByStartedAtDesc(jobType, pageable));
    }

    @PostMapping("/material-price/run-now")
    public ResponseEntity<JobRun> runMaterialPriceNow(Authentication authentication) {
        Long userId = authentication != null && authentication.getPrincipal() instanceof Long ? (Long) authentication.getPrincipal() : null;
        JobRun jr = jobRunService.start("MATERIAL_PRICE_FETCH", null, userId, userId == null ? null : ("user:" + userId), null);
        try {
            MaterialPriceFetchService.FetchResult result = materialPriceFetchService.fetchAndUpsertAll();
            String msg = "failed=" + result.failed();
            JobRun saved = jobRunService.success(jr, result.inserted(), result.updated(), result.failed(), msg, null);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            JobRun saved = jobRunService.failed(jr, e.getMessage(), null);
            return ResponseEntity.status(500).body(saved);
        }
    }
}
