package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.dto.MiitCpJobProgressRequest;
import com.scrap_system.backend_api.dto.MiitCpSyncJobCreateRequest;
import com.scrap_system.backend_api.model.JobRun;
import com.scrap_system.backend_api.repository.JobRunRepository;
import com.scrap_system.backend_api.service.JobRunService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/miit-cp-jobs")
@RequiredArgsConstructor
public class AdminMiitCpJobController {

    private static final String JOB_TYPE = "MIIT_CP_SYNC";

    private final JobRunRepository jobRunRepository;
    private final JobRunService jobRunService;

    @GetMapping
    public ResponseEntity<Page<JobRun>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "startedAt"));
        return ResponseEntity.ok(jobRunRepository.findByJobTypeOrderByStartedAtDesc(JOB_TYPE, pageable));
    }

    @GetMapping("/{runId}")
    public ResponseEntity<JobRun> get(@PathVariable String runId) {
        return jobRunRepository.findByRunId(runId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<JobRun> create(Authentication authentication, @RequestBody MiitCpSyncJobCreateRequest request) {
        if (request == null || request.getPcFrom() == null || request.getPcTo() == null) {
            return ResponseEntity.badRequest().build();
        }
        int pcFrom = request.getPcFrom();
        int pcTo = request.getPcTo();
        if (pcFrom <= 0 || pcTo <= 0 || pcFrom > pcTo) {
            return ResponseEntity.badRequest().build();
        }
        List<String> cpsbList = request.getCpsbList();
        if (cpsbList != null && cpsbList.stream().anyMatch(s -> s != null && s.trim().length() == 1)) {
            return ResponseEntity.badRequest().build();
        }
        int pageSize = request.getPageSize() == null ? 10 : Math.min(Math.max(request.getPageSize(), 1), 50);
        Integer limit = request.getLimit();
        if (limit != null) limit = Math.min(Math.max(limit, 1), 20000);
        boolean headful = request.getHeadful() == null || request.getHeadful();

        Long userId = authentication != null && authentication.getPrincipal() instanceof Long ? (Long) authentication.getPrincipal() : null;
        String actorName = userId == null ? null : ("user:" + userId);

        Map<String, Object> config = new HashMap<>();
        config.put("pcFrom", pcFrom);
        config.put("pcTo", pcTo);
        config.put("qymc", trimOrNull(request.getQymc()));
        config.put("cpsb", trimOrNull(request.getCpsb()));
        config.put("clxh", trimOrNull(request.getClxh()));
        config.put("clmc", trimOrNull(request.getClmc()));
        config.put("cpsbList", request.getCpsbList() == null ? null : request.getCpsbList().stream().filter(s -> s != null && !s.trim().isEmpty()).map(String::trim).toList());
        config.put("qymcList", request.getQymcList() == null ? null : request.getQymcList().stream().filter(s -> s != null && !s.trim().isEmpty()).map(String::trim).toList());
        config.put("pageSize", pageSize);
        config.put("limit", limit);
        config.put("headful", headful);

        Map<String, Object> detailsMap = new HashMap<>();
        detailsMap.put("config", config);
        detailsMap.put("progress", Map.of("stage", "CREATED"));

        String detailsJson = JsonUtil.toJson(detailsMap);

        JobRun jr = jobRunService.createPending(JOB_TYPE, null, userId, actorName, detailsJson);
        jr.setMessage("pending");
        return ResponseEntity.ok(jobRunRepository.save(jr));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<JobRun>> pending(@RequestParam(defaultValue = "10") int limit) {
        int lim = Math.min(Math.max(limit, 1), 50);
        PageRequest pageable = PageRequest.of(0, lim, Sort.by(Sort.Direction.ASC, "startedAt"));
        List<JobRun> rows = jobRunRepository.findByJobTypeOrderByStartedAtDesc(JOB_TYPE, pageable).getContent();
        rows = rows.stream().filter(j -> "PENDING".equals(j.getStatus())).toList();
        return ResponseEntity.ok(rows);
    }

    @PostMapping("/{runId}/claim")
    @Transactional
    public ResponseEntity<JobRun> claim(Authentication authentication, @PathVariable String runId, @RequestParam(required = false) String worker) {
        Optional<JobRun> opt = jobRunRepository.findByRunId(runId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        JobRun jr = opt.get();
        if (!JOB_TYPE.equals(jr.getJobType())) return ResponseEntity.badRequest().build();
        if (!"PENDING".equals(jr.getStatus())) return ResponseEntity.status(409).body(jr);

        Long userId = authentication != null && authentication.getPrincipal() instanceof Long ? (Long) authentication.getPrincipal() : null;
        String w = trimOrNull(worker);
        String msg = w == null ? "running" : ("running@" + w);
        JobRun saved = jobRunService.markRunning(jr, msg, jr.getDetailsJson());
        saved.setActorUserId(userId);
        if (userId != null) saved.setActorName("user:" + userId);
        return ResponseEntity.ok(jobRunRepository.save(saved));
    }

    @PostMapping("/{runId}/progress")
    @Transactional
    public ResponseEntity<JobRun> progress(@PathVariable String runId, @RequestBody MiitCpJobProgressRequest request) {
        Optional<JobRun> opt = jobRunRepository.findByRunId(runId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        JobRun jr = opt.get();
        if (!JOB_TYPE.equals(jr.getJobType())) return ResponseEntity.badRequest().build();
        if (!"RUNNING".equals(jr.getStatus())) return ResponseEntity.status(409).body(jr);
        String detailsJson = request == null ? null : request.getDetailsJson();
        String msg = request == null ? null : request.getMessage();
        Integer inserted = request == null ? null : request.getInserted();
        Integer updated = request == null ? null : request.getUpdated();
        Integer skipped = request == null ? null : request.getSkipped();
        return ResponseEntity.ok(jobRunService.updateProgress(jr, inserted, updated, skipped, msg, detailsJson));
    }

    @PostMapping("/{runId}/complete")
    @Transactional
    public ResponseEntity<JobRun> complete(@PathVariable String runId, @RequestBody MiitCpJobProgressRequest request) {
        Optional<JobRun> opt = jobRunRepository.findByRunId(runId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        JobRun jr = opt.get();
        if (!JOB_TYPE.equals(jr.getJobType())) return ResponseEntity.badRequest().build();
        Integer inserted = request == null ? jr.getInsertedCount() : request.getInserted();
        Integer updated = request == null ? jr.getUpdatedCount() : request.getUpdated();
        Integer skipped = request == null ? jr.getSkippedCount() : request.getSkipped();
        String msg = request == null ? jr.getMessage() : request.getMessage();
        String detailsJson = request == null ? jr.getDetailsJson() : request.getDetailsJson();
        return ResponseEntity.ok(jobRunService.success(jr, inserted, updated, skipped, msg, detailsJson));
    }

    @PostMapping("/{runId}/fail")
    @Transactional
    public ResponseEntity<JobRun> fail(@PathVariable String runId, @RequestBody MiitCpJobProgressRequest request) {
        Optional<JobRun> opt = jobRunRepository.findByRunId(runId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        JobRun jr = opt.get();
        if (!JOB_TYPE.equals(jr.getJobType())) return ResponseEntity.badRequest().build();
        String msg = request == null ? "failed" : trimOrNull(request.getMessage());
        String detailsJson = request == null ? null : request.getDetailsJson();
        return ResponseEntity.ok(jobRunService.failed(jr, msg, detailsJson));
    }

    @PostMapping("/{runId}/retry")
    @Transactional
    public ResponseEntity<JobRun> retry(Authentication authentication, @PathVariable String runId) {
        Optional<JobRun> opt = jobRunRepository.findByRunId(runId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        JobRun oldJob = opt.get();
        if (!JOB_TYPE.equals(oldJob.getJobType())) return ResponseEntity.badRequest().build();
        
        // Extract config and failed items from old job details
        String detailsJson = oldJob.getDetailsJson();
        if (detailsJson == null) return ResponseEntity.badRequest().build();
        
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> details = mapper.readValue(detailsJson, Map.class);
            Map<String, Object> oldConfig = (Map<String, Object>) details.get("config");
            
            // Check for failed items in progress.result or just progress
            // Based on python code: payload = {"config": config, "progress": {"stage": "DONE", "result": result}}
            // result has "failed_items"
            
            List<Map<String, Object>> failedItems = null;
            Map<String, Object> progress = (Map<String, Object>) details.get("progress");
            if (progress != null) {
                Map<String, Object> result = (Map<String, Object>) progress.get("result");
                if (result != null) {
                    failedItems = (List<Map<String, Object>>) result.get("failed_items");
                }
            }
            
            if (failedItems == null || failedItems.isEmpty()) {
                // If no specific failed items found, maybe we just want to re-run the whole job?
                // But the user specifically asked for "retry failed records".
                // If there are no failed records recorded, we can't do partial retry.
                // Let's return 400 with message if possible, or just bad request.
                // For now, if no failed items, we fall back to re-running the whole job config?
                // No, user requirement is specific.
                return ResponseEntity.badRequest().body(null);
            }
            
            // Construct new config with retryItems
            Map<String, Object> newConfig = new HashMap<>();
            if (oldConfig != null) {
                newConfig.putAll(oldConfig);
            }
            newConfig.put("retryItems", failedItems);
            
            Map<String, Object> newDetailsMap = new HashMap<>();
            newDetailsMap.put("config", newConfig);
            newDetailsMap.put("progress", Map.of("stage", "CREATED", "retryFrom", runId));
            
            Long userId = authentication != null && authentication.getPrincipal() instanceof Long ? (Long) authentication.getPrincipal() : null;
            String actorName = userId == null ? null : ("user:" + userId);
            
            JobRun jr = jobRunService.createPending(JOB_TYPE, null, userId, actorName, JsonUtil.toJson(newDetailsMap));
            jr.setMessage("retry of " + runId);
            return ResponseEntity.ok(jobRunRepository.save(jr));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static class JsonUtil {
        private static String toJson(Object obj) {
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
