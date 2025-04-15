package com.fastgpt.ai.service.impl;

import com.fastgpt.ai.dto.KbDataDTO;
import com.fastgpt.ai.dto.KnowledgeBaseDTO;
import com.fastgpt.ai.dto.request.KbDataCreateRequest;
import com.fastgpt.ai.dto.request.KnowledgeBaseCreateRequest;
import com.fastgpt.ai.dto.request.VectorSearchRequest;
import com.fastgpt.ai.entity.KbData;
import com.fastgpt.ai.entity.KnowledgeBase;
import com.fastgpt.ai.exception.ResourceNotFoundException;
import com.fastgpt.ai.mapper.KbDataMapper;
import com.fastgpt.ai.mapper.KnowledgeBaseMapper;
import com.fastgpt.ai.repository.KbDataRepository;
import com.fastgpt.ai.repository.KnowledgeBaseRepository;
import com.fastgpt.ai.service.KnowledgeBaseService;
import com.fastgpt.ai.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KbDataRepository kbDataRepository;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KbDataMapper kbDataMapper;
    private final VectorService vectorService;

    @Override
    @Transactional
    public KnowledgeBaseDTO createKnowledgeBase(KnowledgeBaseCreateRequest request) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.toEntity(request);
        
        // Generate a unique kbId
        knowledgeBase.setKbId(UUID.randomUUID().toString());
        
        // Set initial counts
        knowledgeBase.setFileCount(0);
        knowledgeBase.setKbDataCount(0);
        
        // Set timestamps
        LocalDateTime now = LocalDateTime.now();
        knowledgeBase.setCreateTime(now);
        knowledgeBase.setUpdateTime(now);
        
        // If no collection ID provided, generate one
        if (knowledgeBase.getCollectionId() == null || knowledgeBase.getCollectionId().isEmpty()) {
            knowledgeBase.setCollectionId("kb_" + UUID.randomUUID().toString().replace("-", ""));
        }
        
        KnowledgeBase savedKnowledgeBase = knowledgeBaseRepository.save(knowledgeBase);
        
        return knowledgeBaseMapper.toDTO(savedKnowledgeBase);
    }

    @Override
    public KnowledgeBaseDTO getKnowledgeBaseById(String kbId) {
        return knowledgeBaseRepository.findByKbId(kbId)
                .map(knowledgeBaseMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge Base", "kbId", kbId));
    }

    @Override
    public List<KnowledgeBaseDTO> getKnowledgeBasesByUserId(String userId) {
        return knowledgeBaseMapper.toDTOList(knowledgeBaseRepository.findByUserId(userId));
    }

    @Override
    public List<KnowledgeBaseDTO> getAccessibleKnowledgeBases(String userId) {
        return knowledgeBaseMapper.toDTOList(knowledgeBaseRepository.findByUserIdOrSharedIsTrue(userId));
    }

    @Override
    @Transactional
    public KnowledgeBaseDTO updateKnowledgeBase(String kbId, KnowledgeBaseDTO update) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findByKbId(kbId)
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge Base", "kbId", kbId));
        
        // Update fields
        if (update.getName() != null) {
            knowledgeBase.setName(update.getName());
        }
        
        if (update.getTags() != null) {
            knowledgeBase.setTags(update.getTags());
        }
        
        if (update.getIntro() != null) {
            knowledgeBase.setIntro(update.getIntro());
        }
        
        if (update.getVectorModel() != null) {
            knowledgeBase.setVectorModel(update.getVectorModel());
        }
        
        if (update.getCollectionId() != null) {
            knowledgeBase.setCollectionId(update.getCollectionId());
        }
        
        if (update.getShared() != null) {
            knowledgeBase.setShared(update.getShared());
        }
        
        if (update.getCustomInfo() != null) {
            knowledgeBase.setCustomInfo(update.getCustomInfo());
        }
        
        if (update.getModelInfo() != null) {
            knowledgeBase.setModelInfo(update.getModelInfo());
        }
        
        // Update timestamp
        knowledgeBase.setUpdateTime(LocalDateTime.now());
        
        KnowledgeBase updatedKnowledgeBase = knowledgeBaseRepository.save(knowledgeBase);
        
        return knowledgeBaseMapper.toDTO(updatedKnowledgeBase);
    }

    @Override
    @Transactional
    public void deleteKnowledgeBase(String kbId) {
        if (!knowledgeBaseRepository.findByKbId(kbId).isPresent()) {
            throw new ResourceNotFoundException("Knowledge Base", "kbId", kbId);
        }
        
        // Delete all KB data first
        kbDataRepository.deleteByKbId(kbId);
        
        // Then delete the KB itself
        knowledgeBaseRepository.deleteByKbId(kbId);
    }

    @Override
    @Transactional
    public KbDataDTO addData(KbDataCreateRequest request) {
        // Verify knowledge base exists
        KnowledgeBase kb = knowledgeBaseRepository.findByKbId(request.getKbId())
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge Base", "kbId", request.getKbId()));
        
        KbData kbData = kbDataMapper.toEntity(request);
        
        // Generate a unique dataId
        kbData.setDataId(UUID.randomUUID().toString());
        
        // Set timestamps
        LocalDateTime now = LocalDateTime.now();
        kbData.setCreateTime(now);
        kbData.setUpdateTime(now);
        
        // Count tokens if not provided
        if (kbData.getQTokens() == null) {
            kbData.setQTokens(vectorService.countTokens(kbData.getQ()));
        }
        
        if (kbData.getATokens() == null && kbData.getA() != null) {
            kbData.setATokens(vectorService.countTokens(kbData.getA()));
        }
        
        // Generate vector if not provided
        if (kbData.getVector() == null || kbData.getVector().isEmpty()) {
            String vectorModel = request.getVectorModel();
            if (vectorModel == null || vectorModel.isEmpty()) {
                vectorModel = kb.getVectorModel();
            }
            kbData.setVector(vectorService.generateEmbedding(kbData.getQ(), vectorModel));
            kbData.setVectorModel(vectorModel);
        }
        
        // If no collection ID provided, use the KB's collection ID
        if (kbData.getCollectionId() == null || kbData.getCollectionId().isEmpty()) {
            kbData.setCollectionId(kb.getCollectionId());
        }
        
        KbData savedKbData = kbDataRepository.save(kbData);
        
        // Update KB data count
        kb.setKbDataCount(kb.getKbDataCount() + 1);
        if (kbData.getFileId() != null && !kbData.getFileId().isEmpty()) {
            kb.setFileCount(kb.getFileCount() + 1);
        }
        kb.setUpdateTime(now);
        knowledgeBaseRepository.save(kb);
        
        return kbDataMapper.toDTO(savedKbData);
    }

    @Override
    public KbDataDTO getDataById(String dataId) {
        return kbDataRepository.findByDataId(dataId)
                .map(kbDataMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("KB Data", "dataId", dataId));
    }

    @Override
    public List<KbDataDTO> getDataByKbId(String kbId) {
        // Verify knowledge base exists
        if (!knowledgeBaseRepository.findByKbId(kbId).isPresent()) {
            throw new ResourceNotFoundException("Knowledge Base", "kbId", kbId);
        }
        
        return kbDataMapper.toDTOList(kbDataRepository.findByKbId(kbId));
    }

    @Override
    @Transactional
    public void deleteData(String dataId) {
        KbData kbData = kbDataRepository.findByDataId(dataId)
                .orElseThrow(() -> new ResourceNotFoundException("KB Data", "dataId", dataId));
        
        String kbId = kbData.getKbId();
        String fileId = kbData.getFileId();
        
        // Delete the data
        kbDataRepository.deleteByDataId(dataId);
        
        // Update KB counts
        KnowledgeBase kb = knowledgeBaseRepository.findByKbId(kbId)
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge Base", "kbId", kbId));
        
        kb.setKbDataCount(kb.getKbDataCount() - 1);
        if (fileId != null && !fileId.isEmpty()) {
            kb.setFileCount(kb.getFileCount() - 1);
        }
        kb.setUpdateTime(LocalDateTime.now());
        knowledgeBaseRepository.save(kb);
    }

    @Override
    public List<KbDataDTO> search(VectorSearchRequest request) {
        return vectorService.search(request);
    }
} 