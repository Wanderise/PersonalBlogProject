package com.third.parser.reader;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
@Component
public class TxtDocumentReader implements FileDocumentReader {
    @Override
    public List<Document> read(MultipartFile multipartFile) {
        Tika tika = new Tika();
        try {
            String text = tika.parseToString(multipartFile.getInputStream());
            return List.of(new Document(text));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TikaException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean support(String type) {
        return "txt".equalsIgnoreCase(type);
    }
}
