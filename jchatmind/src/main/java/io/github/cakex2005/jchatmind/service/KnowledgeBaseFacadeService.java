package io.github.cakex2005.jchatmind.service;

import io.github.cakex2005.jchatmind.model.request.CreateKnowledgeBaseRequest;
import io.github.cakex2005.jchatmind.model.request.UpdateKnowledgeBaseRequest;
import io.github.cakex2005.jchatmind.model.response.CreateKnowledgeBaseResponse;
import io.github.cakex2005.jchatmind.model.response.GetKnowledgeBasesResponse;

public interface KnowledgeBaseFacadeService {
    GetKnowledgeBasesResponse getKnowledgeBases();

    CreateKnowledgeBaseResponse createKnowledgeBase(CreateKnowledgeBaseRequest request);

    void deleteKnowledgeBase(String knowledgeBaseId);

    void updateKnowledgeBase(String knowledgeBaseId, UpdateKnowledgeBaseRequest request);
}
