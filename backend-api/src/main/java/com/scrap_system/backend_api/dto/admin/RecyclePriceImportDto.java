package com.scrap_system.backend_api.dto.admin;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class RecyclePriceImportDto {

    @ExcelProperty("材料类型")
    private String materialName;

    @ExcelProperty("回收单价")
    private Double price;

    @ExcelProperty("单位")
    private String unit;
}
