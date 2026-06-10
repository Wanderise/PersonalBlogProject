package com.third.controller;

import com.third.common.result.Result;
import com.third.pojo.dto.UploadRequestDTO;
import com.third.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/file")
@Tag(name = "上传文件")
public class FileController {

    @Autowired
    private FileService fileService;

    @Value("${r2.public-domain:#{null}}")
    private String publicDomain;

    @PostMapping("upload/url")
    @Operation(summary = "获取上传url")
    public Result<Map<String, String>> getUploadUrl(@RequestBody UploadRequestDTO uploadRequestDTO) {
        String objectKey = uploadRequestDTO.getObjectKey();
        String contentType = uploadRequestDTO.getContentType();
        String uploadPresignedUrl = fileService.getUploadPresignedUrl(objectKey, contentType);
        log.info("上传url: {}", uploadPresignedUrl);

        String publicUrl = (publicDomain == null || "http://example.com".equals(publicDomain))
                ? null : publicDomain.replaceAll("/$", "") + "/" + objectKey;
        Map<String, String> map = new HashMap<>();
        map.put("uploadUrl", uploadPresignedUrl);
        map.put("publicUrl", publicUrl);
        return Result.success(map);
    }

    @Operation(summary = "获取下载url")
    @GetMapping("download/url")
    public Result<Map<String, String>> getDownloadUrl(@RequestParam String objectKey) {
        String downloadPresignedUrl = fileService.getDownloadPresignedUrl(objectKey);
        Map<String, String> map = new HashMap<>();
        map.put("downloadUrl", downloadPresignedUrl);
        return Result.success(map);
    }

}
