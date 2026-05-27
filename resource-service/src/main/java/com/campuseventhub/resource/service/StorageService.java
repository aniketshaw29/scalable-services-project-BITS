package com.campuseventhub.resource.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@Slf4j
public class StorageService {

    private final Path uploadDir;

    public StorageService(@Value("${resource.upload-dir:uploads}") String uploadDir) throws IOException {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
    }

    public void store(String storageKey, MultipartFile file) throws IOException {
        Path target = uploadDir.resolve(storageKey);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
    }

    public byte[] load(String storageKey) throws IOException {
        Path target = uploadDir.resolve(storageKey);
        return Files.readAllBytes(target);
    }

    public void delete(String storageKey) throws IOException {
        Path target = uploadDir.resolve(storageKey);
        Files.deleteIfExists(target);
    }
}
