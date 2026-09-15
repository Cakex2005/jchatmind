package com.kama.jchatmind.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MultiChatClientConfig {
    // deepseek
//    @Bean("deepseek-chat")
//    public ChatClient deepSeekChatClient(DeepSeekChatModel deepSeekChatModel) {
//        return ChatClient.create(deepSeekChatModel);
//    }

//    // zhipuai
//    @Bean("glm-4.6")
//    public ChatClient zhiPuAiChatClient(ZhiPuAiChatModel zhiPuAiChatModel) {
//        return ChatClient.create(zhiPuAiChatModel);
//    }

    // openai
    @Bean("openai")
    public ChatClient openaiChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.create(openAiChatModel);
    }
}
