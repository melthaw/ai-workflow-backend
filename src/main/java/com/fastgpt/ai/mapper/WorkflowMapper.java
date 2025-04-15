package com.fastgpt.ai.mapper;

import com.fastgpt.ai.dto.workflow.ConnectionDTO;
import com.fastgpt.ai.dto.workflow.NodeDefDTO;
import com.fastgpt.ai.dto.workflow.WorkflowDTO;
import com.fastgpt.ai.dto.request.WorkflowCreateRequest;
import com.fastgpt.ai.dto.request.WorkflowUpdateRequest;
import com.fastgpt.ai.entity.Workflow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WorkflowMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workflowId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "nodes", expression = "java(nodeDefsToJson(request.getNodes()))")
    @Mapping(target = "edges", expression = "java(connectionsToJson(request.getEdges()))")
    @Mapping(target = "defaultInputs", source = "defaultInputs")
    @Mapping(target = "config", source = "config")
    Workflow toEntity(WorkflowCreateRequest request);
    
    @Mapping(target = "nodes", expression = "java(jsonToNodeDefs(workflow.getNodes()))")
    @Mapping(target = "edges", expression = "java(jsonToConnections(workflow.getEdges()))")
    WorkflowDTO toDTO(Workflow workflow);
    
    List<WorkflowDTO> toDTOList(List<Workflow> workflows);
    
    /**
     * Convert NodeDefDTO objects to Map representations for storage
     * @param nodeDefs List of node definitions
     * @return List of map representations
     */
    default List<Map<String, Object>> nodeDefsToJson(List<NodeDefDTO> nodeDefs) {
        if (nodeDefs == null) {
            return null;
        }
        
        return nodeDefs.stream()
                .map(nodeDefDTO -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", nodeDefDTO.getId());
                    map.put("type", nodeDefDTO.getType());
                    map.put("name", nodeDefDTO.getName());
                    map.put("x", nodeDefDTO.getX());
                    map.put("y", nodeDefDTO.getY());
                    map.put("data", nodeDefDTO.getData());
                    map.put("isEntry", nodeDefDTO.getIsEntry());
                    map.put("isRequired", nodeDefDTO.getIsRequired());
                    return map;
                })
                .toList();
    }
    
    /**
     * Convert ConnectionDTO objects to Map representations for storage
     * @param connections List of connection definitions
     * @return List of map representations
     */
    default List<Map<String, Object>> connectionsToJson(List<ConnectionDTO> connections) {
        if (connections == null) {
            return null;
        }
        
        return connections.stream()
                .map(connectionDTO -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("sourceNodeId", connectionDTO.getSourceNodeId());
                    map.put("sourceHandle", connectionDTO.getSourceHandle());
                    map.put("targetNodeId", connectionDTO.getTargetNodeId());
                    map.put("targetHandle", connectionDTO.getTargetHandle());
                    return map;
                })
                .toList();
    }
    
    /**
     * Convert Map representations to NodeDefDTO objects
     * @param jsonNodes List of map representations
     * @return List of node definitions
     */
    default List<NodeDefDTO> jsonToNodeDefs(List<Map<String, Object>> jsonNodes) {
        if (jsonNodes == null) {
            return null;
        }
        
        return jsonNodes.stream()
                .map(json -> {
                    NodeDefDTO nodeDefDTO = new NodeDefDTO();
                    nodeDefDTO.setId((String) json.get("id"));
                    nodeDefDTO.setType((String) json.get("type"));
                    nodeDefDTO.setName((String) json.get("name"));
                    nodeDefDTO.setX((Integer) json.get("x"));
                    nodeDefDTO.setY((Integer) json.get("y"));
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) json.get("data");
                    nodeDefDTO.setData(data);
                    
                    nodeDefDTO.setIsEntry((Boolean) json.get("isEntry"));
                    nodeDefDTO.setIsRequired((Boolean) json.get("isRequired"));
                    
                    return nodeDefDTO;
                })
                .toList();
    }
    
    /**
     * Convert Map representations to ConnectionDTO objects
     * @param jsonConnections List of map representations
     * @return List of connection definitions
     */
    default List<ConnectionDTO> jsonToConnections(List<Map<String, Object>> jsonConnections) {
        if (jsonConnections == null) {
            return null;
        }
        
        return jsonConnections.stream()
                .map(json -> {
                    ConnectionDTO connectionDTO = new ConnectionDTO();
                    connectionDTO.setSourceNodeId((String) json.get("sourceNodeId"));
                    connectionDTO.setSourceHandle((String) json.get("sourceHandle"));
                    connectionDTO.setTargetNodeId((String) json.get("targetNodeId"));
                    connectionDTO.setTargetHandle((String) json.get("targetHandle"));
                    
                    return connectionDTO;
                })
                .toList();
    }
} 