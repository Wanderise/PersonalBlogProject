package com.third.parser.reader;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
@Component
public class WordDocumentReader implements FileDocumentReader {

    @Override
    public List<Document> read(MultipartFile multipartFile) {
        try {
            XWPFDocument xwpfDocument = new XWPFDocument(multipartFile.getInputStream());
            String text = new XWPFWordExtractor(xwpfDocument).getText();
            Document document = new Document(text);
            return List.of(document);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public boolean support(String type) {
        return type != null && (type.contains("officedocument.wordprocessingml.document")
                || type.equals("docx"));
    }
}
