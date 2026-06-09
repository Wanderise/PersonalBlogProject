package com.third.parser.reader;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
public interface FileDocumentReader {
    List<Document> read(MultipartFile multipartFile);
    boolean support(String type);

}
