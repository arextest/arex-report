package com.arextest.report.model.dao.mongodb.entity;

import lombok.Data;

import java.util.List;

@Data
public class FSNode {
    private String nodeName;
    private Integer nodeType;
    private String infoId;
    private List<FSNode> children;
}
