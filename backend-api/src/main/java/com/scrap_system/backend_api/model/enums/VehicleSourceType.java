package com.scrap_system.backend_api.model.enums;

public enum VehicleSourceType {
    CRAWLED("抓取"),
    MANUAL("手动录入"),
    EDITED("抓取后手动编辑");

    private final String description;

    VehicleSourceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
