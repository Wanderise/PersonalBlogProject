package com.third.pojo.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class RagFileVO {
    private Integer id;
    private String title;
    private String fileType;
    private String r2Key;
    private String status;
    private LocalDate gmtCreate;
}
