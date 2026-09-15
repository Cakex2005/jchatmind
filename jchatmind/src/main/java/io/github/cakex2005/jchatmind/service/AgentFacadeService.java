package io.github.cakex2005.jchatmind.service;

import io.github.cakex2005.jchatmind.model.request.CreateAgentRequest;
import io.github.cakex2005.jchatmind.model.request.UpdateAgentRequest;
import io.github.cakex2005.jchatmind.model.response.CreateAgentResponse;
import io.github.cakex2005.jchatmind.model.response.GetAgentsResponse;

public interface AgentFacadeService {
    GetAgentsResponse getAgents();

    CreateAgentResponse createAgent(CreateAgentRequest request);

    void deleteAgent(String agentId);

    void updateAgent(String agentId, UpdateAgentRequest request);
}
