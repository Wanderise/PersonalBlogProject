package com.third.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RagFile {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private Integer knowledgeBaseId;
    private String title;
    private String fileType;
    private String r2Key;
    private String status;
    private LocalDate gmtCreate;
    private String hash;
    private Double version;

}
