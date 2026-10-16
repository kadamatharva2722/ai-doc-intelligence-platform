package com.enterprise.docintel.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Path;

public interface FileStorageService {
    String storeFile(MultipartFile file) throws IOException;
    Path loadFile(String fileName);
    String extractText(String fileName, String fileType) throws IOException;
}
