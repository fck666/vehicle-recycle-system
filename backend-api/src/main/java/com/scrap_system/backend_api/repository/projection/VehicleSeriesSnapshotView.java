package com.scrap_system.backend_api.repository.projection;

import java.math.BigDecimal;

public interface VehicleSeriesSnapshotView {
    Long getId();

    String getBrand();

    String getModel();

    Integer getModelYear();

    String getManufacturerName();

    String getVehicleType();

    String getFuelType();

    BigDecimal getCurbWeight();

    Integer getWheelbaseMm();

    String getTrademark();

    String getProductNo();
}
