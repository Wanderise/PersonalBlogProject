package com.third.parser.reader;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
@Component
public class WordDocumentReader implements FileDocumentReader {

    @Override
    public List<Document> read(Resource resource, String contentType) {
        try (var input = resource.getInputStream()) {
            String text;
            if ("application/msword".equalsIgnoreCase(contentType) || "doc".equalsIgnoreCase(contentType)) {
                try (HWPFDocument document = new HWPFDocument(input);
                     WordExtractor extractor = new WordExtractor(document)) {
                    text = extractor.getText();
                }
            } else {
                try (XWPFDocument document = new XWPFDocument(input);
                     XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                    text = extractor.getText();
                }
            }
            return List.of(new Document(text));
        } catch (IOException e) {
            throw new RuntimeException("Word document parsing failed", e);
        }

    }

    @Override
    public boolean support(String type) {
        return type != null && (type.contains("officedocument.wordprocessingml.document")
                || type.equalsIgnoreCase("docx")
                || type.equalsIgnoreCase("application/msword")
                || type.equalsIgnoreCase("doc"));
    }
}
