package com.arextest.report.core.business.filesystem;

import com.arextest.report.core.repository.FSCaseRepository;
import com.arextest.report.core.repository.FSInterfaceRepository;
import com.arextest.report.core.repository.FSTreeRepository;
import com.arextest.report.model.api.contracts.filesystem.FSAddItemRequestType;
import com.arextest.report.model.api.contracts.filesystem.FSAddItemResponseType;
import com.arextest.report.model.api.contracts.filesystem.FSQueryCaseRequestType;
import com.arextest.report.model.api.contracts.filesystem.FSQueryCaseResponseType;
import com.arextest.report.model.api.contracts.filesystem.FSQueryInterfaceRequestType;
import com.arextest.report.model.api.contracts.filesystem.FSQueryInterfaceResponseType;
import com.arextest.report.model.api.contracts.filesystem.FSQueryWorkspaceRequestType;
import com.arextest.report.model.api.contracts.filesystem.FSQueryWorkspaceResponseType;
import com.arextest.report.model.api.contracts.filesystem.FSQueryWorkspacesRequestType;
import com.arextest.report.model.api.contracts.filesystem.FSQueryWorkspacesResponseType;
import com.arextest.report.model.api.contracts.filesystem.FSRemoveItemRequestType;
import com.arextest.report.model.api.contracts.filesystem.FSRemoveItemResponseType;
import com.arextest.report.model.api.contracts.filesystem.FSRenameRequestType;
import com.arextest.report.model.api.contracts.filesystem.FSSaveCaseRequestType;
import com.arextest.report.model.api.contracts.filesystem.FSSaveCaseResponseType;
import com.arextest.report.model.api.contracts.filesystem.FSSaveInterfaceRequestType;
import com.arextest.report.model.api.contracts.filesystem.FSSaveInterfaceResponseType;
import com.arextest.report.model.api.contracts.filesystem.FSTreeType;
import com.arextest.report.model.dto.WorkspaceDto;
import com.arextest.report.model.dto.filesystem.FSCaseDto;
import com.arextest.report.model.dto.filesystem.FSInterfaceDto;
import com.arextest.report.model.dto.filesystem.FSNodeDto;
import com.arextest.report.model.dto.filesystem.FSTreeDto;
import com.arextest.report.model.mapper.FSCaseMapper;
import com.arextest.report.model.mapper.FSInterfaceMapper;
import com.arextest.report.model.mapper.FSTreeMapper;
import com.arextest.report.model.mapper.WorkspaceMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FileSystemService {
    private static final String DEFAULT_WORKSPACE_NAME = "MyWorkSpace";

    @Resource
    private FSTreeRepository fsTreeRepository;

    @Resource
    private FSInterfaceRepository fsInterfaceRepository;

    @Resource
    private FSCaseRepository fsCaseRepository;

    @Resource
    private ItemInfoFactory itemInfoFactory;

    public FSAddItemResponseType addItem(FSAddItemRequestType request) {
        FSAddItemResponseType response = new FSAddItemResponseType();

        ItemInfo itemInfo = itemInfoFactory.getItemInfo(request.getNodeType());
        if (itemInfo == null) {
            response.setSuccess(false);
            return response;
        }

        try {
            if (StringUtils.isEmpty(request.getWorkspaceName())) {
                request.setWorkspaceName(DEFAULT_WORKSPACE_NAME);
            }
            if (StringUtils.isEmpty(request.getUserName())) {
                request.setUserName(StringUtils.EMPTY);
            }

            FSTreeDto dto;
            if (StringUtils.isEmpty(request.getId())) {
                dto = new FSTreeDto();
                dto.setWorkspaceName(request.getWorkspaceName());
                dto.setUserName(request.getUserName());
                dto.setRoots(new ArrayList<>());
                dto = fsTreeRepository.initFSTree(dto);
            } else {
                dto = fsTreeRepository.queryFSTreeById(request.getId());
            }

            String infoId = null;
            if (request.getParentPath() == null || request.getParentPath().length == 0) {
                FSNodeDto nodeDto = new FSNodeDto();
                nodeDto.setNodeName(request.getNodeName());
                infoId = itemInfo.saveItem();
                nodeDto.setInfoId(infoId);
                nodeDto.setNodeType(request.getNodeType());
                dto.getRoots().add(0, nodeDto);
            } else {
                String[] nodes = request.getParentPath();

                FSNodeDto current = findByInfoId(dto.getRoots(), nodes[0]);
                if (current == null) {
                    response.setSuccess(false);
                    return response;
                }

                boolean error = false;
                for (int i = 1; i < nodes.length; i++) {
                    String node = nodes[i];
                    if (current.getChildren() == null || current.getChildren().size() == 0) {
                        error = true;
                        break;
                    }
                    current = findByInfoId(current.getChildren(), node);

                    if (current == null) {
                        error = true;
                        break;
                    }
                }
                if (!error) {
                    if (current.getChildren() == null) {
                        current.setChildren(new ArrayList<>());
                    }
                    FSNodeDto newNodeDto = new FSNodeDto();
                    newNodeDto.setNodeName(request.getNodeName());
                    infoId = itemInfo.saveItem();
                    newNodeDto.setInfoId(infoId);
                    newNodeDto.setNodeType(request.getNodeType());
                    current.getChildren().add(0, newNodeDto);

                } else {
                    response.setSuccess(false);
                    return response;
                }
            }
            fsTreeRepository.updateFSTree(dto);
            response.setInfoId(infoId);
            response.setSuccess(true);

        } catch (Exception e) {
            LOGGER.error("failed to add item to filesystem", e);
            response.setSuccess(false);
        }
        return response;
    }

    public Boolean removeItem(FSRemoveItemRequestType request) {
        FSTreeDto treeDto = fsTreeRepository.queryFSTreeById(request.getId());
        if (treeDto == null) {
            return false;
        }
        List<FSNodeDto> current = treeDto.getRoots();
        if (current == null) {
            return false;
        }

        String[] nodes = request.getRemoveNodePath();
        for (int i = 0; i < nodes.length - 1; i++) {

            String node = nodes[i];
            FSNodeDto find = findByInfoId(current, node);

            if (find == null || find.getChildren() == null) {
                return false;
            }
            current = find.getChildren();
        }

        FSNodeDto needRemove = findByInfoId(current, nodes[nodes.length - 1]);
        removeItems(needRemove);
        current.remove(needRemove);
        fsTreeRepository.updateFSTree(treeDto);
        return true;
    }

    public Boolean rename(FSRenameRequestType request) {

        FSTreeDto fsTreeDto = fsTreeRepository.queryFSTreeById(request.getId());
        if (fsTreeDto == null) {
            return false;
        }
        String[] pathArr = request.getPath();
        List<FSNodeDto> tmp = fsTreeDto.getRoots();
        for (int i = 0; i < pathArr.length - 1; i++) {
            String pathNode = pathArr[i];
            if (tmp == null || tmp.size() == 0) {
                return false;
            }
            FSNodeDto find = findByInfoId(tmp, pathNode);
            if (find == null) {
                return false;
            }
            tmp = find.getChildren();
        }
        String last = pathArr[pathArr.length - 1];
        FSNodeDto dto = findByInfoId(tmp, last);

        if (dto == null) {
            return false;
        }
        dto.setNodeName(request.getNewName());

        fsTreeRepository.updateFSTree(fsTreeDto);
        return true;
    }

    public FSQueryWorkspaceResponseType queryWorkspaceById(FSQueryWorkspaceRequestType request) {
        FSQueryWorkspaceResponseType response = new FSQueryWorkspaceResponseType();
        FSTreeDto treeDto = fsTreeRepository.queryFSTreeById(request.getId());
        FSTreeType treeType = FSTreeMapper.INSTANCE.contractFromDto(treeDto);
        response.setFsTree(treeType);
        return response;
    }

    public FSQueryWorkspacesResponseType queryWorkspacesByUser(FSQueryWorkspacesRequestType request) {
        FSQueryWorkspacesResponseType response = new FSQueryWorkspacesResponseType();
        List<WorkspaceDto> workspaces = fsTreeRepository.queryWorkspacesByUser(request.getUserName());
        response.setWorkspaces(WorkspaceMapper.INSTANCE.contractFromDtoList(workspaces));
        return response;
    }

    public FSSaveInterfaceResponseType saveInterface(FSSaveInterfaceRequestType request) {
        FSSaveInterfaceResponseType response = new FSSaveInterfaceResponseType();
        FSInterfaceDto dto = FSInterfaceMapper.INSTANCE.dtoFromContract(request);
        try {
            fsInterfaceRepository.saveInterface(dto);
        } catch (Exception e) {
            response.setSuccess(false);
        }
        response.setSuccess(true);
        return response;
    }

    public FSQueryInterfaceResponseType queryInterface(FSQueryInterfaceRequestType request) {
        FSInterfaceDto dto = fsInterfaceRepository.queryInterface(request.getId());
        if (dto == null) {
            return new FSQueryInterfaceResponseType();
        }
        return FSInterfaceMapper.INSTANCE.contractFromDto(dto);
    }

    public FSSaveCaseResponseType saveCase(FSSaveCaseRequestType request) {
        FSSaveCaseResponseType response = new FSSaveCaseResponseType();
        FSCaseDto dto = FSCaseMapper.INSTANCE.dtoFromContract(request);
        try {
            fsCaseRepository.saveCase(dto);
        } catch (Exception e) {
            response.setSuccess(false);
        }
        response.setSuccess(true);
        return response;
    }

    public FSQueryCaseResponseType queryCase(FSQueryCaseRequestType request) {
        FSCaseDto dto = fsCaseRepository.queryCase(request.getId());
        if (dto == null) {
            return new FSQueryCaseResponseType();
        }
        return FSCaseMapper.INSTANCE.contractFromDto(dto);
    }

    private void removeItems(FSNodeDto fsNodeDto) {
        if (fsNodeDto == null) {
            return;
        }
        Queue<FSNodeDto> queue = new ArrayDeque<>();
        queue.add(fsNodeDto);

        while (!queue.isEmpty()) {
            FSNodeDto dto = queue.poll();
            ItemInfo itemInfo = itemInfoFactory.getItemInfo(dto.getNodeType());
            itemInfo.removeItem(dto.getInfoId());
            if (dto.getChildren() != null && dto.getChildren().size() > 0) {
                queue.addAll(dto.getChildren());
            }
        }
    }

    private FSNodeDto findByInfoId(List<FSNodeDto> list, String infoId) {
        if (list == null || list.size() == 0) {
            return null;
        }
        List<FSNodeDto> filter = list.stream().filter(f -> f.getInfoId().equals(infoId)).collect(Collectors.toList());
        if (filter == null || filter.size() == 0) {
            return null;
        }
        return filter.get(0);
    }
}
