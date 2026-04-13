package com.pdfeditor.service;

import com.pdfeditor.config.FileStorageConfig;
import com.pdfeditor.exception.FileNotFoundException;
import com.pdfeditor.util.FileValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Service
public class FileManagementService {

    private static final Logger log = LoggerFactory.getLogger(FileManagementService.class);

    private final FileStorageConfig config;
    private final Map<String, FileRecord> fileRegistry = new ConcurrentHashMap<>();

    public FileManagementService(FileStorageConfig config) {
        this.config = config;
    }

    /**
     * Stores uploaded file and returns its unique ID.
     */
    public String store(MultipartFile file) throws IOException {
        FileValidator.validatePdf(file);

        String fileId = UUID.randomUUID().toString();
        String sanitizedName = FileValidator.sanitizeFileName(file.getOriginalFilename());
        Path targetPath = config.getUploadPath().resolve(fileId + "_" + sanitizedName);

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        FileRecord record = new FileRecord(fileId, sanitizedName, targetPath, file.getSize(), Instant.now());
        fileRegistry.put(fileId, record);

        log.info("Stored file: {} -> {}", fileId, sanitizedName);
        return fileId;
    }

    /**
     * Stores a processed file (byte array) and returns its unique ID.
     */
    public String storeProcessed(byte[] data, String fileName) throws IOException {
        String fileId = UUID.randomUUID().toString();
        String sanitizedName = FileValidator.sanitizeFileName(fileName);
        Path targetPath = config.getUploadPath().resolve(fileId + "_" + sanitizedName);

        Files.write(targetPath, data);

        FileRecord record = new FileRecord(fileId, sanitizedName, targetPath, data.length, Instant.now());
        fileRegistry.put(fileId, record);

        log.info("Stored processed file: {} -> {}", fileId, sanitizedName);
        return fileId;
    }

    /**
     * Retrieves file path by ID.
     */
    public Path getFilePath(String fileId) {
        FileRecord record = fileRegistry.get(fileId);
        if (record == null) {
            throw new FileNotFoundException("File not found: " + fileId);
        }
        if (!Files.exists(record.path())) {
            fileRegistry.remove(fileId);
            throw new FileNotFoundException("File no longer exists on disk: " + fileId);
        }
        return record.path();
    }

    /**
     * Gets file record by ID.
     */
    public FileRecord getFileRecord(String fileId) {
        FileRecord record = fileRegistry.get(fileId);
        if (record == null) {
            throw new FileNotFoundException("File not found: " + fileId);
        }
        return record;
    }

    /**
     * Deletes a file by ID.
     */
    public void delete(String fileId) throws IOException {
        FileRecord record = fileRegistry.remove(fileId);
        if (record != null && Files.exists(record.path())) {
            Files.delete(record.path());
            log.info("Deleted file: {}", fileId);
        }
    }

    /**
     * Lists all stored files.
     */
    public List<Map<String, Object>> listFiles() {
        List<Map<String, Object>> files = new ArrayList<>();
        for (FileRecord record : fileRegistry.values()) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("fileId", record.fileId());
            info.put("fileName", record.fileName());
            info.put("fileSize", record.size());
            info.put("uploadedAt", record.uploadedAt().toString());
            files.add(info);
        }
        return files;
    }

    /**
     * Scheduled cleanup of expired files (runs every 10 minutes).
     */
    @Scheduled(fixedRate = 600000)
    public void cleanupExpired() {
        Instant cutoff = Instant.now().minus(config.getMaxAgeMinutes(), ChronoUnit.MINUTES);
        List<String> expired = new ArrayList<>();

        for (Map.Entry<String, FileRecord> entry : fileRegistry.entrySet()) {
            if (entry.getValue().uploadedAt().isBefore(cutoff)) {
                expired.add(entry.getKey());
            }
        }

        for (String fileId : expired) {
            try {
                delete(fileId);
                log.info("Cleaned up expired file: {}", fileId);
            } catch (IOException e) {
                log.warn("Failed to cleanup file: {}", fileId, e);
            }
        }

        if (!expired.isEmpty()) {
            log.info("Cleaned up {} expired files", expired.size());
        }
    }

    public record FileRecord(String fileId, String fileName, Path path, long size, Instant uploadedAt) {}
}
