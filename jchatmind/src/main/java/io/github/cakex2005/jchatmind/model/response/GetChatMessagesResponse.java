package io.github.cakex2005.jchatmind.model.response;

import io.github.cakex2005.jchatmind.model.vo.ChatMessageVO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetChatMessagesResponse {
    private ChatMessageVO[] chatMessages;
}

