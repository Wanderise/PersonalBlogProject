package com.third.common.config;

import io.qdrant.client.QdrantClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient DeepseekChatClient(DeepSeekChatModel deepSeekChatModel){
        return ChatClient.builder(deepSeekChatModel).build();
    }

}