package com.pdfeditor.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public class FileValidator {

    // PDF magic bytes: %PDF
    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46};

    private FileValidator() {}

    /**
     * Validates that the uploaded file is a valid PDF.
     */
    public static void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }

        String originalName = file.getOriginalFilename();
        if (originalName != null && !originalName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("File must have .pdf extension");
        }

        // Check magic bytes
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[4];
            int read = is.read(header);
            if (read < 4) {
                throw new IllegalArgumentException("File is too small to be a valid PDF");
            }
            for (int i = 0; i < 4; i++) {
                if (header[i] != PDF_MAGIC[i]) {
                    throw new IllegalArgumentException("File does not appear to be a valid PDF (invalid magic bytes)");
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read file for validation", e);
        }
    }

    /**
     * Validates that the uploaded file is an image.
     */
    public static void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty or null");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image (PNG, JPEG, etc.)");
        }
    }

    /**
     * Sanitizes a filename to prevent path traversal.
     */
    public static String sanitizeFileName(String fileName) {
        if (fileName == null) return "unnamed.pdf";
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
