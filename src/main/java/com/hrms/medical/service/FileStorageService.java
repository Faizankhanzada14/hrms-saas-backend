package com.hrms.medical.service;

import com.hrms.exception.BadRequestException;
import com.hrms.medical.config.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/png", "image/jpeg", "image/jpg", "application/pdf");

    private final FileStorageProperties properties;

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Attachment file is empty");
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new BadRequestException("Attachment exceeds the maximum allowed size of "
                    + (properties.getMaxFileSizeBytes() / (1024 * 1024)) + " MB");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Unsupported attachment type. Allowed: PNG, JPEG, PDF");
        }

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment");
        if (originalName.contains("..")) {
            throw new BadRequestException("Invalid file path in attachment name");
        }

        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex);
        }
        String storedName = UUID.randomUUID() + extension;

        try {
            Path uploadPath = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            Path targetPath = uploadPath.resolve(storedName).normalize();
            if (!targetPath.startsWith(uploadPath)) {
                throw new BadRequestException("Invalid attachment destination path");
            }

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return new StoredFile(targetPath.toString(), originalName, file.getContentType());
        } catch (IOException e) {
            log.error("Failed to store attachment", e);
            throw new RuntimeException("Failed to store attachment file", e);
        }
    }

    public record StoredFile(String storedPath, String originalName, String contentType) {
    }
}
