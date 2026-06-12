package com.third.service;

import io.qdrant.client.ConditionFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import io.swagger.models.auth.In;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
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

        qdrantClient.deleteAsync(deletePoints);
    }
}