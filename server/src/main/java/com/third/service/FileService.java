package com.third.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.regex.Pattern;

@Service
@Slf4j
public class FileService {
    private static final int MAX_OBJECT_KEY_LENGTH = 255;
    private static final Pattern SAFE_OBJECT_KEY = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._/-]*$");

    @Autowired
    private S3Presigner s3Presigner;
    @Autowired
    private S3Client s3Client;

    @Value("${r2.bucket}")
    private String bucketName;

    public static void validateObjectKey(String objectKey, String... allowedPrefixes) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("objectKey must not be blank");
        }
        if (objectKey.length() > MAX_OBJECT_KEY_LENGTH) {
            throw new IllegalArgumentException("objectKey is too long");
        }
        if (!SAFE_OBJECT_KEY.matcher(objectKey).matches()
                || objectKey.contains("..")
                || objectKey.contains("//")
                || objectKey.contains("\\")) {
            throw new IllegalArgumentException("invalid objectKey");
        }
        if (allowedPrefixes != null && allowedPrefixes.length > 0
                && Arrays.stream(allowedPrefixes).noneMatch(objectKey::startsWith)) {
            throw new IllegalArgumentException("unsupported objectKey prefix");
        }
    }

    public static String normalizeContentType(String contentType) {
        return (contentType == null || contentType.isBlank())
                ? "application/octet-stream" : contentType.trim();
    }

    public String getUploadPresignedUrl(String objectKey, String contentType) {
        validateObjectKey(objectKey, "images/", "avatars/", "knowledge_base/");
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(normalizeContentType(contentType))
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(objectRequest)
                .build();
        log.debug("created upload presigned url for key={}", objectKey);
        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }

    public String getDownloadPresignedUrl(String objectKey) {
        validateObjectKey(objectKey, "images/", "avatars/", "knowledge_base/");
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(getObjectRequest)
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    public void deleteObject(String objectKey) {
        validateObjectKey(objectKey, "images/", "avatars/", "knowledge_base/");
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }

    public void uploadMarkdown(String objectKey, String markdown) {
        validateObjectKey(objectKey, "knowledge_base/");
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType("text/markdown")
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(markdown.getBytes(StandardCharsets.UTF_8)));
    }

    public void uploadFile(String objectKey, MultipartFile file) throws IOException {
        validateObjectKey(objectKey, "images/", "avatars/", "knowledge_base/");
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(normalizeContentType(file.getContentType()))
                .build();

        long start = System.currentTimeMillis();
        s3Client.putObject(
                request,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );
        log.debug("uploaded object key={} costMs={}", objectKey, System.currentTimeMillis() - start);
    }

    public void uploadFile(String objectKey, byte[] content, String contentType) {
        validateObjectKey(objectKey, "images/", "avatars/", "knowledge_base/");
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(normalizeContentType(contentType))
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(content));
    }

    public byte[] downloadObject(String objectKey) {
        validateObjectKey(objectKey, "images/", "avatars/", "knowledge_base/");
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
        return response.asByteArray();
    }
}
