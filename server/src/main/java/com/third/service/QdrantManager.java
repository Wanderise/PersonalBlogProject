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

        Points.DeletePoints deletePoints = Points.DeletePoints.newBuilder()
                .setCollectionName(collectionName)
                .setPoints(Points.PointsSelector.newBuilder().setFilter(filter).build())
                .build();

        try {
            qdrantClient.deleteAsync(deletePoints).get();
            log.debug("Qdrant delete success: {}={}", field, value);
        } catch (Exception e) {
            if (isMissingCollection(e)) {
                log.warn("Qdrant delete skipped because collection is missing: collection={} {}={}",
                        collectionName, field, value);
                return;
            }
            log.error("Qdrant delete failed: {}={}, reason={}", field, value, e.getMessage(), e);
            throw new IllegalStateException("Qdrant delete failed: " + field + "=" + value, e);
        }
    }

    private boolean isMissingCollection(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null
                    && message.contains("NOT_FOUND")
                    && message.contains("Collection")
                    && message.contains("doesn't exist")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
