package com.arextest.report.model.dao.mongodb.entity;

import lombok.Data;

@Data
public class AuthDao {
    private String authType;
    private Boolean authActive;
    private String token;
}
