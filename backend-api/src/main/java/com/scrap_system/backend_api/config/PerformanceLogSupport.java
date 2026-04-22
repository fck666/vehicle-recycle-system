package com.scrap_system.backend_api.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class PerformanceLogSupport {

    private final PerformanceLogProperties properties;

    public long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    public void logRequest(Logger log, String target, long elapsedMs, String detail) {
        logByThreshold(log, "api-perf", target, elapsedMs, detail, properties.getRequestWarnThresholdMs());
    }

    public void logStep(Logger log, String target, long elapsedMs, String detail) {
        logByThreshold(log, "service-perf", target, elapsedMs, detail, properties.getStepWarnThresholdMs());
    }

    public void logProxy(Logger log, String target, long elapsedMs, String detail) {
        logByThreshold(log, "proxy-perf", target, elapsedMs, detail, properties.getProxyWarnThresholdMs());
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    private void logByThreshold(Logger log, String label, String target, long elapsedMs, String detail, long thresholdMs) {
        if (!properties.isEnabled()) {
            return;
        }
        boolean forceTrace = PerformanceTraceContext.isForceTrace();
        if (!forceTrace && !properties.isAlwaysLog() && elapsedMs < thresholdMs) {
            return;
        }

        if (elapsedMs >= thresholdMs) {
            log.warn("[{}] {} {}ms {}", label, target, elapsedMs, detail);
            return;
        }
        log.info("[{}] {} {}ms {}", label, target, elapsedMs, detail);
    }
}
