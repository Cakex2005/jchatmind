package io.github.cakex2005.jchatmind.model.request;

import io.github.cakex2005.jchatmind.model.dto.AgentDTO;
import lombok.Data;

import java.util.List;

@Data
public class UpdateAgentRequest {
    private String name;
    private String description;
    private String systemPrompt;
    private String model;
    private List<String> allowedTools;
    private List<String> allowedKbs;
    private AgentDTO.ChatOptions chatOptions;
}
