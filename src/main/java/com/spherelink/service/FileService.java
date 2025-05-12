package com.spherelink.service;

import com.spherelink.model.FileRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    private static final String UPLOAD_BASE_DIR = "Uploads";
    private static final String VIEWS_SUBDIR = "users_views_pics";
    private static final String PROFILE_SUBDIR = "users_profile_pics";

    public FileRecord saveFile(MultipartFile file, String prefix, boolean isProfileImage) throws IOException {
        if (file == null || file.isEmpty()) {
            logger.warn("Received empty or null file");
            return null;
        }

        // Determine subdirectory
        String subDir = isProfileImage ? PROFILE_SUBDIR : VIEWS_SUBDIR;
        Path uploadPath = Paths.get(UPLOAD_BASE_DIR, subDir).toAbsolutePath().normalize();

        // Create directory if it doesn't exist
        try {
            Files.createDirectories(uploadPath);
            logger.info("Ensured directory exists: {}", uploadPath);
        } catch (IOException e) {
            logger.error("Failed to create directory {}: {}", uploadPath, e.getMessage());
            throw new IOException("Cannot create upload directory: " + uploadPath, e);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String filename = (prefix != null ? prefix : "") + UUID.randomUUID() + extension;
        Path filePath = uploadPath.resolve(filename);

        // Save file
        try {
            Files.copy(file.getInputStream(), filePath);
            logger.info("Saved file to {}", filePath);

            // Verify file exists
            if (!Files.exists(filePath)) {
                logger.error("File was not created at {}", filePath);
                return null;
            }

            // Return relative path with forward slashes
            String relativePath = String.join("/", UPLOAD_BASE_DIR, subDir, filename);
            return new FileRecord(filename, relativePath);
        } catch (IOException e) {
            logger.error("Failed to save file {}: {}", filePath, e.getMessage());
            return null;
        }
    }
    
    public void deleteFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            logger.warn("Attempted to delete null or empty file path");
            return;
        }

        try {
            // Convert relative path to absolute
            String normalizedPath = filePath.replace("/", java.io.File.separator);
            Path path = Paths.get(normalizedPath).toAbsolutePath().normalize();
            if (Files.exists(path)) {
                Files.delete(path);
                logger.info("Deleted file: {}", path);
            } else {
                logger.warn("File does not exist: {}", path);
            }
        } catch (IOException e) {
            logger.error("Failed to delete file {}: {}", filePath, e.getMessage());
        }
    }
}