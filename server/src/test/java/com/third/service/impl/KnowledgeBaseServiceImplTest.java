package com.third.service.impl;

import com.third.common.exception.NoAuthorization;
import com.third.mapper.KnowledgeBaseMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceImplTest {

    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;
    @Mock
    private VectorStore vectorStore;
    @InjectMocks
    private KnowledgeBaseServiceImpl service;

    @Test
    void queryRejectsKnowledgeBaseOwnedByAnotherUserBeforeVectorSearch() {
        when(knowledgeBaseMapper.selectCount(any())).thenReturn(0L);

        NoAuthorization error = assertThrows(NoAuthorization.class,
                () -> service.queryKnowledgeBase("8", "secret", 42));

        assertEquals(403, error.getCode());
        verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void queryRejectsMalformedKnowledgeBaseIds() {
        NoAuthorization error = assertThrows(NoAuthorization.class,
                () -> service.queryKnowledgeBase("8 or true", "secret", 42));

        assertEquals(400, error.getCode());
        verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
    }
}
