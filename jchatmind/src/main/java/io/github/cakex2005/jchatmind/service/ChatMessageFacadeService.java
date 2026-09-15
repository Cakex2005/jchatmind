package io.github.cakex2005.jchatmind.service;

import io.github.cakex2005.jchatmind.model.dto.ChatMessageDTO;
import io.github.cakex2005.jchatmind.model.request.CreateChatMessageRequest;
import io.github.cakex2005.jchatmind.model.request.UpdateChatMessageRequest;
import io.github.cakex2005.jchatmind.model.response.CreateChatMessageResponse;
import io.github.cakex2005.jchatmind.model.response.GetChatMessagesResponse;

import java.util.List;

public interface ChatMessageFacadeService {
    GetChatMessagesResponse getChatMessagesBySessionId(String sessionId);

    List<ChatMessageDTO> getChatMessagesBySessionIdRecently(String sessionId, int limit);

    CreateChatMessageResponse createChatMessage(CreateChatMessageRequest request);

    CreateChatMessageResponse createChatMessage(ChatMessageDTO chatMessageDTO);

    CreateChatMessageResponse agentCreateChatMessage(CreateChatMessageRequest request);

    CreateChatMessageResponse appendChatMessage(String chatMessageId, String appendContent);

    void deleteChatMessage(String chatMessageId);

    void updateChatMessage(String chatMessageId, UpdateChatMessageRequest request);
}
