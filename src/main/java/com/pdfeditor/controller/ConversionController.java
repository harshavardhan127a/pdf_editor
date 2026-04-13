package com.pdfeditor.controller;

import com.pdfeditor.dto.ApiResponse;
import com.pdfeditor.service.ConversionService;
import com.pdfeditor.service.FileManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/convert")
public class ConversionController {

    private static final Logger log = LoggerFactory.getLogger(ConversionController.class);

    private final ConversionService conversionService;
    private final FileManagementService fileService;

    public ConversionController(ConversionService conversionService, FileManagementService fileService) {
        this.conversionService = conversionService;
        this.fileService = fileService;
    }

    @PostMapping("/{fileId}/to-docx")
    public ResponseEntity<byte[]> convertToDocx(@PathVariable String fileId) throws IOException {
        byte[] docxBytes = conversionService.convertToDocx(fileId);
        FileManagementService.FileRecord record = fileService.getFileRecord(fileId);
        String baseName = record.fileName().replaceAll("\\.pdf$", "");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + baseName + ".docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(docxBytes);
    }

    @PostMapping("/{fileId}/to-pptx")
    public ResponseEntity<byte[]> convertToPptx(@PathVariable String fileId) throws IOException {
        byte[] pptxBytes = conversionService.convertToPptx(fileId);
        FileManagementService.FileRecord record = fileService.getFileRecord(fileId);
        String baseName = record.fileName().replaceAll("\\.pdf$", "");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + baseName + ".pptx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.presentationml.presentation"))
                .body(pptxBytes);
    }
}
