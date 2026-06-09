package com.third.parser.factory;

import com.third.parser.reader.FileDocumentReader;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentReaderFactory {
    private final List<FileDocumentReader> readers;
    public DocumentReaderFactory(List<FileDocumentReader> readers) {
        this.readers = readers;
    }
    public FileDocumentReader getReader(String type) {
        return readers.stream()
                .filter(r -> r.support(type))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Document type not supported: " + type));

    }
}
