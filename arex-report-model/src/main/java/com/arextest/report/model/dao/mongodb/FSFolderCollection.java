package com.arextest.report.model.dao.mongodb;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "FSFolder")
public class FSFolderCollection extends ModelBase {
    private String parentId;
    private Integer parentNodeType;
}
