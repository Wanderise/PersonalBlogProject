package com.third.pojo.dto;

import com.third.pojo.vo.RagFileVO;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class RagFileDTO {
    private List<MultipartFile> files;
    private List<Integer> articleIds;
    private Integer knowledgeBaseId;
}
