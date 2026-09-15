package io.github.cakex2005.jchatmind.model.response;

import io.github.cakex2005.jchatmind.model.vo.KnowledgeBaseVO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetKnowledgeBasesResponse {
    private KnowledgeBaseVO[] knowledgeBases;
}

