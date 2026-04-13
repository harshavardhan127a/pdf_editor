package com.pdfeditor.controller;

import com.pdfeditor.dto.ApiResponse;
import com.pdfeditor.service.FileManagementService;
import com.pdfeditor.service.MetadataService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileManagementService fileService;
    private final MetadataService metadataService;

    public FileController(FileManagementService fileService, MetadataService metadataService) {
        this.fileService = fileService;
        this.metadataService = metadataService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listFiles() {
        return ResponseEntity.ok(ApiResponse.success(fileService.listFiles()));
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable String fileId) throws IOException {
        fileService.delete(fileId);
        return ResponseEntity.ok(ApiResponse.success("File deleted successfully", null));
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String fileId) throws IOException {
        Path filePath = fileService.getFilePath(fileId);
        FileManagementService.FileRecord record = fileService.getFileRecord(fileId);
        byte[] fileBytes = Files.readAllBytes(filePath);

        String contentType = record.fileName().endsWith(".pdf") ? "application/pdf" : "application/octet-stream";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + record.fileName() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(fileBytes);
    }

    @GetMapping("/{fileId}/metadata")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMetadata(@PathVariable String fileId) {
        Map<String, Object> metadata = metadataService.extractMetadata(fileId);
        return ResponseEntity.ok(ApiResponse.success(metadata));
    }
}
