package com.scrap_system.backend_api.dto;

import lombok.Data;

import java.util.List;

@Data
public class MiitCpSyncJobCreateRequest {
    private Integer pcFrom;
    private Integer pcTo;
    private String qymc;
    private String clxh;
    private String clmc;
    private List<String> cpsbList;
    private List<String> qymcList;
    private Integer pageSize;
    private Integer limit;
    private Boolean headful;

    public List<String> getCpsbList() {
        return cpsbList;
    }

    public void setCpsbList(List<String> cpsbList) {
        this.cpsbList = cpsbList;
    }

    public List<String> getQymcList() {
        return qymcList;
    }

    public void setQymcList(List<String> qymcList) {
        this.qymcList = qymcList;
    }
}

