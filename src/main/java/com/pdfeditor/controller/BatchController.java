package com.pdfeditor.controller;

import com.pdfeditor.dto.ApiResponse;
import com.pdfeditor.service.BatchProcessingService;
import com.pdfeditor.service.FileManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batch")
public class BatchController {

    private static final Logger log = LoggerFactory.getLogger(BatchController.class);

    private final BatchProcessingService batchService;
    private final FileManagementService fileService;

    public BatchController(BatchProcessingService batchService, FileManagementService fileService) {
        this.batchService = batchService;
        this.fileService = fileService;
    }

    @PostMapping("/merge")
    public ResponseEntity<ApiResponse<Map<String, String>>> batchMerge(@RequestBody Map<String, List<String>> request)
            throws IOException {
        List<String> fileIds = request.get("fileIds");
        if (fileIds == null || fileIds.size() < 2) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("At least two file IDs required"));
        }

        byte[] merged = batchService.batchMerge(fileIds);
        String newFileId = fileService.storeProcessed(merged, "batch_merged.pdf");

        return ResponseEntity.ok(ApiResponse.success("Batch merge complete",
                Map.of("fileId", newFileId)));
    }

    @PostMapping("/convert")
    public ResponseEntity<byte[]> batchConvert(@RequestBody Map<String, Object> request) throws IOException {
        @SuppressWarnings("unchecked")
        List<String> fileIds = (List<String>) request.get("fileIds");
        String format = (String) request.get("format");

        if (fileIds == null || fileIds.isEmpty()) {
            throw new IllegalArgumentException("File IDs are required");
        }
        if (format == null || format.isBlank()) {
            throw new IllegalArgumentException("Target format is required (docx or pptx)");
        }

        byte[] zipBytes = batchService.batchConvert(fileIds, format);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"batch_converted.zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zipBytes);
    }
}
