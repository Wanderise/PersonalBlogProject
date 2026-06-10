package com.third.parser.reader;

import com.third.common.enumerate.RespondCode;
import com.third.common.exception.BaseExcpetion;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Component
public class PdfDocumentReader implements FileDocumentReader {


    @Override
    public boolean support(String type) {
        return type != null && (type.equals("application/pdf") || type.equals("pdf"));
    }

    @Override
    public List<Document> read(MultipartFile multipartFile) {
        try {
            Resource resource = new InputStreamResource(multipartFile.getInputStream());
            DocumentReader reader = new PagePdfDocumentReader(resource);
            return reader.get();
        } catch (IOException e) {
            throw new BaseExcpetion(RespondCode.NOT_FOUND);
        }
    }
}
