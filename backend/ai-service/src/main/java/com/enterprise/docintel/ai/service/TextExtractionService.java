package com.enterprise.docintel.ai.service;

import com.enterprise.docintel.common.exception.BadRequestException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class TextExtractionService {

    private final Path fileStorageLocation;

    public TextExtractionService(@Value("${app.upload.dir:./uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String extractText(String fileName, String fileType) throws IOException {
        Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
        File file = filePath.toFile();

        if (!file.exists()) {
            throw new BadRequestException("File not found on disk: " + fileName);
        }

        return switch (fileType.toUpperCase()) {
            case "PDF" -> extractPdfText(file);
            case "DOCX" -> extractDocxText(file);
            case "TXT" -> extractTxtText(file);
            default -> throw new BadRequestException("Unsupported file type: " + fileType);
        };
    }

    private String extractPdfText(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        }
    }

    private String extractDocxText(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractTxtText(File file) throws IOException {
        return Files.readString(file.toPath());
    }
}
