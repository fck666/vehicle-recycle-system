package com.scrap_system.backend_api.repository.projection;

import java.math.BigDecimal;

public interface VehicleValuationSourceView {
    Long getId();

    Long getVehicleId();

    BigDecimal getSteelWeight();

    BigDecimal getAluminumWeight();

    BigDecimal getCopperWeight();

    BigDecimal getBatteryWeight();

    BigDecimal getOtherWeight();

    String getDetailsJson();
}
