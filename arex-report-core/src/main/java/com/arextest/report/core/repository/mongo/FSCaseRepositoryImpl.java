package com.arextest.report.core.repository.mongo;

import com.arextest.report.core.repository.FSCaseRepository;
import com.arextest.report.core.repository.mongo.util.MongoHelper;
import com.arextest.report.model.dao.mongodb.FSCaseCollection;
import com.arextest.report.model.dto.filesystem.FSCaseDto;
import com.arextest.report.model.mapper.FSCaseMapper;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class FSCaseRepositoryImpl implements FSCaseRepository {

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public String initCase(String parentId, Integer parentNodeType) {
        FSCaseCollection dao = new FSCaseCollection();
        MongoHelper.initInsertObject(dao);
        dao.setParentId(parentId);
        dao.setParentNodeType(parentNodeType);
        dao = mongoTemplate.insert(dao);
        return dao.getId();
    }
    @Override
    public Boolean removeCases(String id) {
        Query query = Query.query(Criteria.where(DASH_ID).is(id));
        FSCaseCollection dao = mongoTemplate.findAndRemove(query, FSCaseCollection.class);
        return true;
    }
    @Override
    public FSCaseDto saveCase(FSCaseDto dto) {
        Query query = Query.query(Criteria.where(DASH_ID).is(dto.getId()));
        Update update = MongoHelper.getUpdate();

        FSCaseCollection dao = FSCaseMapper.INSTANCE.daoFromDto(dto);
        MongoHelper.appendFullProperties(update, dao);

        FSCaseCollection result = mongoTemplate.findAndModify(query,
                update,
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                FSCaseCollection.class);
        return FSCaseMapper.INSTANCE.dtoFromDao(result);
    }
    @Override
    public FSCaseDto queryCase(String id) {
        FSCaseCollection dao = mongoTemplate.findById(new ObjectId(id), FSCaseCollection.class);
        if (dao == null) {
            return null;
        }
        return FSCaseMapper.INSTANCE.dtoFromDao(dao);
    }
    @Override
    public List<FSCaseDto> queryCases(List<String> ids) {
        List<ObjectId> objectIds = ids.stream().map(id -> new ObjectId(id)).collect(Collectors.toList());
        Query query = Query.query(Criteria.where(DASH_ID).in(objectIds));
        List<FSCaseCollection> daos = mongoTemplate.find(query, FSCaseCollection.class);
        return daos.stream().map(FSCaseMapper.INSTANCE::dtoFromDao).collect(Collectors.toList());
    }

    @Override
    public String duplicate(FSCaseDto dto) {
        FSCaseCollection dao = FSCaseMapper.INSTANCE.daoFromDto(dto);
        MongoHelper.initInsertObject(dao);
        dao = mongoTemplate.insert(dao);
        return dao.getId();
    }
}
