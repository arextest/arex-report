package com.arextest.report.model.dto.filesystem;

import com.arextest.report.model.dto.KeyValuePairDto;
import lombok.Data;

import java.util.List;

@Data
public class FSCaseDto {
    private String id;
    private String preRequestScript;
    private String testScript;
    private BodyDto body;
    private List<KeyValuePairDto> headers;
    private List<KeyValuePairDto> params;
    private AuthDto auth;
    private String parentId;
    private Integer parentNodeType;
}
