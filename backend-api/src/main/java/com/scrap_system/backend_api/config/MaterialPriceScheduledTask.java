package com.scrap_system.backend_api.config;

import com.scrap_system.backend_api.model.JobRun;
import com.scrap_system.backend_api.service.JobRunService;
import com.scrap_system.backend_api.service.MaterialPriceFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MaterialPriceScheduledTask {

    private final MaterialPriceFetchService materialPriceFetchService;
    private final JobRunService jobRunService;

    @Value("${app.material-price-fetch.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${app.material-price-fetch.cron:0 10 2 * * *}", zone = "${app.material-price-fetch.zone:Asia/Shanghai}")
    public void run() {
        if (!enabled) {
            return;
        }
        JobRun jr = jobRunService.start("MATERIAL_PRICE_FETCH", null, null, "system", null);
        try {
            MaterialPriceFetchService.FetchResult result = materialPriceFetchService.fetchAndUpsertAll();
            String msg = "failed=" + result.failed();
            jobRunService.success(jr, result.inserted(), result.updated(), result.failed(), msg, null);
            log.info("material price fetch done: inserted={}, updated={}, failed={}", result.inserted(), result.updated(), result.failed());
        } catch (Exception e) {
            jobRunService.failed(jr, e.getMessage(), null);
            throw e;
        }
    }
}
