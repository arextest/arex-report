package com.arextest.report.model.dto;

import lombok.Data;

import java.util.Date;
import java.util.Map;


@Data
public class PlanItemDto {
    
    private Long planItemId;
    
    private Long planId;
    
    private Long operationId;
    
    private String operationName;
    
    private String serviceName;
    
    private Integer status;
    
    private Long replayStartTime;
    private Long replayEndTime;
    
    private Integer totalCaseCount;

    private Date dataCreateTime;


    
    private Map<String, Integer> cases;
    
    private Map<String, Integer> failCases;
    
    private Map<String, Integer> errorCases;
}
