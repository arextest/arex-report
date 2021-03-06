package com.arextest.report.core.repository.mongo;


import com.arextest.report.core.repository.FSFolderRepository;
import com.arextest.report.core.repository.mongo.util.MongoHelper;
import com.arextest.report.model.dao.mongodb.FSFolderCollection;
import com.arextest.report.model.dto.filesystem.FSFolderDto;
import com.arextest.report.model.mapper.FSFolderMapper;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class FSFolderRepositoryImpl implements FSFolderRepository {

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public String initFolder(String parentId, Integer parentNodeType) {
        FSFolderCollection dao = new FSFolderCollection();
        MongoHelper.initInsertObject(dao);
        dao.setParentId(parentId);
        dao.setParentNodeType(parentNodeType);
        dao = mongoTemplate.insert(dao);
        return dao.getId();
    }
    @Override
    public Boolean removeFolder(String id) {
        Query query = Query.query(Criteria.where(DASH_ID).is(id));
        try {
            mongoTemplate.findAndRemove(query, FSFolderCollection.class);
            return true;
        } catch (Exception e) {
            LOGGER.error("failed to remove folder.", e);
            return false;
        }

    }
    @Override
    public FSFolderDto queryById(String id) {
        FSFolderCollection dao = mongoTemplate.findById(new ObjectId(id), FSFolderCollection.class);
        return FSFolderMapper.INSTANCE.dtoFromDao(dao);
    }

    @Override
    public String duplicate(FSFolderDto dto) {
        FSFolderCollection dao = FSFolderMapper.INSTANCE.daoFromDto(dto);
        MongoHelper.initInsertObject(dao);
        dao = mongoTemplate.insert(dao);
        return dao.getId();
    }
}
