package io.github.cakex2005.jchatmind.model.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateKnowledgeBaseResponse {
    private String knowledgeBaseId;
}

