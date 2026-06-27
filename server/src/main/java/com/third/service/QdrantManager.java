package com.third.service;

import io.qdrant.client.ConditionFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class QdrantManager {

    private final QdrantClient qdrantClient;

    @Value("${spring.ai.vectorstore.qdrant.collection-name}")
    private String collectionName;

    public void deleteByKbId(Integer kbId) {
        deleteByField("kb_id", kbId);
    }

    public void deleteByDocumentId(Integer documentId) {
        deleteByField("document_id", documentId);
    }

    public void deleteByVersionId(Integer versionId) {
        deleteByField("version_id", versionId);
    }

    private void deleteByField(String field, Integer value) {
        Points.Filter filter = Points.Filter.newBuilder()
                .addMust(ConditionFactory.match(field, value))
                .build();

        Points.DeletePoints deletePoints =
                Points.DeletePoints.newBuilder()
                        .setCollectionName(collectionName)
                        .setPoints(
                                Points.PointsSelector.newBuilder()
                                        .setFilter(filter)
                                        .build()
                        )
                        .build();

        try {
            qdrantClient.deleteAsync(deletePoints).get();
            log.debug("Qdrant delete success: {}={}", field, value);
        } catch (Exception e) {
            log.error("Qdrant 删除失败: {}={}, 原因: {}", field, value, e.getMessage(), e);
            throw new IllegalStateException("Qdrant 删除失败: " + field + "=" + value, e);
        }
    }
}
