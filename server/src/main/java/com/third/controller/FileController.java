package com.third.controller;

import com.third.common.result.Result;
import com.third.pojo.dto.UploadRequestDTO;
import com.third.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/file")
@Tag(name = "File")
public class FileController {
    private static final long MAX_IMAGE_SIZE = 5L * 1024L * 1024L;
    private static final String[] PUBLIC_UPLOAD_PREFIXES = {"images/", "avatars/"};

    @Autowired
    private FileService fileService;

    @Value("${r2.public-domain:#{null}}")
    private String publicDomain;

    private String buildPublicUrl(String objectKey) {
        return (publicDomain == null || publicDomain.isBlank() || "http://example.com".equals(publicDomain))
                ? null : publicDomain.replaceAll("/$", "") + "/" + objectKey;
    }

    private String normalizeContentType(String contentType) {
        return FileService.normalizeContentType(contentType);
    }

    private boolean isAllowedImage(String contentType) {
        String type = normalizeContentType(contentType).toLowerCase(Locale.ROOT);
        return type.equals(MediaType.IMAGE_JPEG_VALUE)
                || type.equals(MediaType.IMAGE_PNG_VALUE)
                || type.equals(MediaType.IMAGE_GIF_VALUE)
                || type.equals("image/webp");
    }

    @PostMapping("upload/url")
    @Operation(summary = "Create upload presigned URL")
    public Result<Map<String, String>> getUploadUrl(@RequestBody UploadRequestDTO uploadRequestDTO) {
        String objectKey = uploadRequestDTO.getObjectKey();
        String contentType = normalizeContentType(uploadRequestDTO.getContentType());
        FileService.validateObjectKey(objectKey, PUBLIC_UPLOAD_PREFIXES);
        if (!isAllowedImage(contentType)) {
            throw new IllegalArgumentException("unsupported image type");
        }
        String uploadPresignedUrl = fileService.getUploadPresignedUrl(objectKey, contentType);

        Map<String, String> map = new HashMap<>();
        map.put("uploadUrl", uploadPresignedUrl);
        map.put("publicUrl", buildPublicUrl(objectKey));
        return Result.success(map);
    }

    @PostMapping(value = "upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file through server")
    public Result<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file,
                                                  @RequestParam("objectKey") String objectKey,
                                                  @RequestParam(value = "contentType", required = false) String contentType) throws Exception {
        String effectiveContentType = normalizeContentType(
                contentType == null || contentType.isBlank() ? file.getContentType() : contentType);
        if (file.isEmpty()) {
            throw new IllegalArgumentException("file must not be empty");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("image size exceeds limit");
        }
        FileService.validateObjectKey(objectKey, PUBLIC_UPLOAD_PREFIXES);
        if (!isAllowedImage(effectiveContentType)) {
            throw new IllegalArgumentException("unsupported image type");
        }

        fileService.uploadFile(objectKey, file);

        Map<String, String> map = new HashMap<>();
        map.put("objectKey", objectKey);
        map.put("publicUrl", buildPublicUrl(objectKey));
        return Result.success(map);
    }

    @Operation(summary = "Create download presigned URL")
    @GetMapping("download/url")
    public Result<Map<String, String>> getDownloadUrl(@RequestParam String objectKey) {
        FileService.validateObjectKey(objectKey, "images/", "avatars/", "knowledge_base/");
        String downloadPresignedUrl = fileService.getDownloadPresignedUrl(objectKey);
        Map<String, String> map = new HashMap<>();
        map.put("downloadUrl", downloadPresignedUrl);
        return Result.success(map);
    }
}
