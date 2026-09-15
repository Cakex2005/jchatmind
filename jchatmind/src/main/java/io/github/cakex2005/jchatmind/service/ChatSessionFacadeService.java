package io.github.cakex2005.jchatmind.service;

import io.github.cakex2005.jchatmind.model.request.CreateChatSessionRequest;
import io.github.cakex2005.jchatmind.model.request.UpdateChatSessionRequest;
import io.github.cakex2005.jchatmind.model.response.CreateChatSessionResponse;
import io.github.cakex2005.jchatmind.model.response.GetChatSessionResponse;
import io.github.cakex2005.jchatmind.model.response.GetChatSessionsResponse;

public interface ChatSessionFacadeService {
    GetChatSessionsResponse getChatSessions();

    GetChatSessionResponse getChatSession(String chatSessionId);

    GetChatSessionsResponse getChatSessionsByAgentId(String agentId);

    CreateChatSessionResponse createChatSession(CreateChatSessionRequest request);

    void deleteChatSession(String chatSessionId);

    void updateChatSession(String chatSessionId, UpdateChatSessionRequest request);
}
