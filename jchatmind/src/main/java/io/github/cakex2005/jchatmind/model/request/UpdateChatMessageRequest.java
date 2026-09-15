package io.github.cakex2005.jchatmind.model.request;

import io.github.cakex2005.jchatmind.model.dto.ChatMessageDTO;
import lombok.Data;

@Data
public class UpdateChatMessageRequest {
    private String content;
    private ChatMessageDTO.MetaData metadata;
}

