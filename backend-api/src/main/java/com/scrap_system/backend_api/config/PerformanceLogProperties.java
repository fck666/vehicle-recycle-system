package com.scrap_system.backend_api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.performance")
@Getter
@Setter
public class PerformanceLogProperties {
    private boolean enabled = true;
    private boolean alwaysLog = false;
    private long requestWarnThresholdMs = 800;
    private long stepWarnThresholdMs = 300;
    private long proxyWarnThresholdMs = 500;
}
