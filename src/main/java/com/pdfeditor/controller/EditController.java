package com.pdfeditor.controller;

import com.pdfeditor.dto.ApiResponse;
import com.pdfeditor.dto.TextInsertRequest;
import com.pdfeditor.service.FileManagementService;
import com.pdfeditor.service.PdfEditingService;
import com.pdfeditor.util.FileValidator;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/edit")
public class EditController {

    private static final Logger log = LoggerFactory.getLogger(EditController.class);

    private final PdfEditingService editingService;
    private final FileManagementService fileService;

    public EditController(PdfEditingService editingService, FileManagementService fileService) {
        this.editingService = editingService;
        this.fileService = fileService;
    }

    @PostMapping("/{fileId}/text")
    public ResponseEntity<ApiResponse<Map<String, String>>> insertText(
            @PathVariable String fileId,
            @Valid @RequestBody TextInsertRequest request) throws IOException {

        byte[] result = editingService.insertText(fileId, request);
        String newFileId = fileService.storeProcessed(result, "text_added.pdf");

        return ResponseEntity.ok(ApiResponse.success("Text inserted successfully",
                Map.of("fileId", newFileId)));
    }

    @PostMapping("/{fileId}/image")
    public ResponseEntity<ApiResponse<Map<String, String>>> insertImage(
            @PathVariable String fileId,
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "100") float x,
            @RequestParam(defaultValue = "500") float y,
            @RequestParam(defaultValue = "0") float width,
            @RequestParam(defaultValue = "0") float height) throws IOException {

        FileValidator.validateImage(imageFile);

        byte[] result = editingService.insertImage(fileId, imageFile, pageNumber, x, y, width, height);
        String newFileId = fileService.storeProcessed(result, "image_added.pdf");

        return ResponseEntity.ok(ApiResponse.success("Image inserted successfully",
                Map.of("fileId", newFileId)));
    }
}
