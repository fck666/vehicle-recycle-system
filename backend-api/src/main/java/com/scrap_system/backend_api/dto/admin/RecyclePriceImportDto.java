package com.scrap_system.backend_api.dto.admin;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RecyclePriceImportDto {

    private Long id;

    @ExcelProperty("材料类型")
    private String materialName;

    @ExcelProperty("回收单价")
    private Double price;

    @ExcelProperty("单位")
    private String unit;

    private LocalDate effectiveDate;
}
