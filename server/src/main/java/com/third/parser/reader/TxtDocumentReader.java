package com.third.parser.reader;

import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
@Component
public class TxtDocumentReader implements FileDocumentReader {
    @Override
    public List<Document> read(Resource resource, String contentType) {
        try {
            String text = resource.getContentAsString(StandardCharsets.UTF_8);
            return List.of(new Document(text));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean support(String type) {
        return type != null && (type.equals("text/plain") || type.equals("text/plain;charset=UTF-8")
                || type.equalsIgnoreCase("txt"));
    }
}
