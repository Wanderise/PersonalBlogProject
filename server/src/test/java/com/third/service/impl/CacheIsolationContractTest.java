package com.third.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.Cacheable;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CacheIsolationContractTest {

    @Test
    void agentPromptCacheIncludesUserAndAgent() throws Exception {
        Method method = AgentServiceImpl.class.getMethod("resolveSystemPrompt", Integer.class, Integer.class);
        assertEquals("#userId + ':' + #agentId", method.getAnnotation(Cacheable.class).key());
    }

    @Test
    void knowledgeBaseFilesCacheIncludesUserAndKnowledgeBase() throws Exception {
        Method method = KnowledgeBaseServiceImpl.class.getMethod("getRagFiles", Integer.class, Integer.class);
        assertEquals("#userId + ':' + #id", method.getAnnotation(Cacheable.class).key());
    }

    @Test
    void articleVersionCacheIncludesUserArticleAndVersion() throws Exception {
        Method method = ArticleServiceImpl.class.getMethod(
                "getVersion", Integer.class, Integer.class, Integer.class);
        assertEquals("#userId + ':' + #id + ':' + #versionId",
                method.getAnnotation(Cacheable.class).key());
    }
}
