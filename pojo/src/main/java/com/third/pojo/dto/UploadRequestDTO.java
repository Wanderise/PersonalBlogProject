package com.third.pojo.dto;

import lombok.Data;

@Data
public class UploadRequestDTO {
    private String objectKey;
    private String contentType;
}
