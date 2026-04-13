package com.pdfeditor.controller;

import com.pdfeditor.dto.*;
import com.pdfeditor.service.FileManagementService;
import com.pdfeditor.service.PdfProcessingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    private static final Logger log = LoggerFactory.getLogger(PdfController.class);

    private final PdfProcessingService pdfService;
    private final FileManagementService fileService;

    public PdfController(PdfProcessingService pdfService, FileManagementService fileService) {
        this.pdfService = pdfService;
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadPdf(@RequestParam("file") MultipartFile file) throws IOException {
        String fileId = fileService.store(file);
        Map<String, String> result = Map.of(
                "fileId", fileId,
                "fileName", file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown.pdf"
        );
        return ResponseEntity.ok(ApiResponse.success("File uploaded successfully", result));
    }

    @PostMapping("/upload-multiple")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> uploadMultiple(
            @RequestParam("files") MultipartFile[] files) throws IOException {
        List<Map<String, String>> results = new ArrayList<>();
        for (MultipartFile file : files) {
            String fileId = fileService.store(file);
            results.add(Map.of(
                    "fileId", fileId,
                    "fileName", file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown.pdf"
            ));
        }
        return ResponseEntity.ok(ApiResponse.success("Files uploaded successfully", results));
    }

    @GetMapping("/{fileId}/info")
    public ResponseEntity<ApiResponse<PdfInfoResponse>> getPdfInfo(@PathVariable String fileId) {
        PdfInfoResponse info = pdfService.getInfo(fileId);
        return ResponseEntity.ok(ApiResponse.success(info));
    }

    @PostMapping("/merge")
    public ResponseEntity<byte[]> mergePdfs(@Valid @RequestBody MergeRequest request) throws IOException {
        byte[] merged = pdfService.merge(request.getFileIds());
        String outputName = request.getOutputFileName() != null ? request.getOutputFileName() : "merged.pdf";

        // Store the merged file
        String fileId = fileService.storeProcessed(merged, outputName);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + outputName + "\"")
                .header("X-File-Id", fileId)
                .contentType(MediaType.APPLICATION_PDF)
                .body(merged);
    }

    @PostMapping("/{fileId}/split")
    public ResponseEntity<ApiResponse<List<String>>> splitPdf(
            @PathVariable String fileId,
            @RequestBody(required = false) SplitRequest request) throws IOException {

        List<int[]> pageRanges = request != null ? request.getPageRanges() : null;
        List<byte[]> parts = pdfService.split(fileId, pageRanges);

        List<String> partFileIds = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            String partId = fileService.storeProcessed(parts.get(i), "split_part_" + (i + 1) + ".pdf");
            partFileIds.add(partId);
        }

        return ResponseEntity.ok(ApiResponse.success("PDF split into " + parts.size() + " parts", partFileIds));
    }

    @PostMapping("/{fileId}/reorder")
    public ResponseEntity<ApiResponse<Map<String, String>>> reorderPages(
            @PathVariable String fileId,
            @Valid @RequestBody ReorderRequest request) throws IOException {

        byte[] result = pdfService.reorderPages(fileId, request.getPageOrder());
        String newFileId = fileService.storeProcessed(result, "reordered.pdf");

        return ResponseEntity.ok(ApiResponse.success("Pages reordered successfully",
                Map.of("fileId", newFileId)));
    }

    @PostMapping("/{fileId}/delete-pages")
    public ResponseEntity<ApiResponse<Map<String, String>>> deletePages(
            @PathVariable String fileId,
            @RequestBody DeletePagesRequest request) throws IOException {

        byte[] result = pdfService.deletePages(fileId, request.getPageNumbers());
        String newFileId = fileService.storeProcessed(result, "pages_deleted.pdf");

        return ResponseEntity.ok(ApiResponse.success("Pages deleted successfully",
                Map.of("fileId", newFileId)));
    }

    @PostMapping("/{fileId}/add-blank-page")
    public ResponseEntity<ApiResponse<Map<String, String>>> addBlankPage(
            @PathVariable String fileId,
            @RequestParam(defaultValue = "-1") int afterPage) throws IOException {

        byte[] result = pdfService.addBlankPage(fileId, afterPage);
        String newFileId = fileService.storeProcessed(result, "page_added.pdf");

        return ResponseEntity.ok(ApiResponse.success("Blank page added",
                Map.of("fileId", newFileId)));
    }

    @GetMapping("/{fileId}/extract-text")
    public ResponseEntity<ApiResponse<Map<String, String>>> extractText(@PathVariable String fileId) {
        String text = pdfService.extractText(fileId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("text", text)));
    }

    @PostMapping("/{fileId}/search")
    public ResponseEntity<ApiResponse<SearchResult>> searchPdf(
            @PathVariable String fileId,
            @Valid @RequestBody SearchRequest request) {

        SearchResult result = pdfService.searchText(fileId, request.getQuery(), request.isCaseSensitive());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String fileId) throws IOException {
        java.nio.file.Path filePath = fileService.getFilePath(fileId);
        FileManagementService.FileRecord record = fileService.getFileRecord(fileId);
        byte[] fileBytes = java.nio.file.Files.readAllBytes(filePath);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + record.fileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(fileBytes);
    }

    @GetMapping("/{fileId}/view")
    public ResponseEntity<byte[]> viewPdf(@PathVariable String fileId) throws IOException {
        java.nio.file.Path filePath = fileService.getFilePath(fileId);
        byte[] fileBytes = java.nio.file.Files.readAllBytes(filePath);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(fileBytes);
    }
}
