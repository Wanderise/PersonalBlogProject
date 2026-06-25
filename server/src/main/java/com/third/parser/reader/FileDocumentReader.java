package com.third.parser.reader;

import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface FileDocumentReader {
    List<Document> read(Resource resource, String contentType);
    boolean support(String type);

}
