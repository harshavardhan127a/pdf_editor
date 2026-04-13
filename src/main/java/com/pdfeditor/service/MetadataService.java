package com.pdfeditor.service;

import com.pdfeditor.exception.PdfProcessingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Service
public class MetadataService {

    private static final Logger log = LoggerFactory.getLogger(MetadataService.class);

    private final FileManagementService fileService;

    public MetadataService(FileManagementService fileService) {
        this.fileService = fileService;
    }

    /**
     * Extract detailed metadata from a PDF.
     */
    public Map<String, Object> extractMetadata(String fileId) {
        Path filePath = fileService.getFilePath(fileId);

        try (PDDocument doc = Loader.loadPDF(filePath.toFile())) {
            Map<String, Object> metadata = new LinkedHashMap<>();

            // Document Info
            PDDocumentInformation info = doc.getDocumentInformation();
            if (info != null) {
                Map<String, String> docInfo = new LinkedHashMap<>();
                putIfNotNull(docInfo, "title", info.getTitle());
                putIfNotNull(docInfo, "author", info.getAuthor());
                putIfNotNull(docInfo, "subject", info.getSubject());
                putIfNotNull(docInfo, "keywords", info.getKeywords());
                putIfNotNull(docInfo, "creator", info.getCreator());
                putIfNotNull(docInfo, "producer", info.getProducer());
                if (info.getCreationDate() != null)
                    docInfo.put("creationDate", info.getCreationDate().getTime().toString());
                if (info.getModificationDate() != null)
                    docInfo.put("modificationDate", info.getModificationDate().getTime().toString());

                // Custom metadata keys
                for (String key : info.getMetadataKeys()) {
                    if (!docInfo.containsKey(key)) {
                        putIfNotNull(docInfo, key, info.getCustomMetadataValue(key));
                    }
                }
                metadata.put("documentInfo", docInfo);
            }

            // Page Info
            int pageCount = doc.getNumberOfPages();
            metadata.put("pageCount", pageCount);
            metadata.put("encrypted", doc.isEncrypted());

            // Individual page dimensions
            List<Map<String, Object>> pages = new ArrayList<>();
            for (int i = 0; i < pageCount; i++) {
                PDPage page = doc.getPage(i);
                PDRectangle mediaBox = page.getMediaBox();
                int rotation = page.getRotation();

                Map<String, Object> pageInfo = new LinkedHashMap<>();
                pageInfo.put("pageNumber", i + 1);
                pageInfo.put("width", mediaBox.getWidth());
                pageInfo.put("height", mediaBox.getHeight());
                pageInfo.put("rotation", rotation);
                pages.add(pageInfo);
            }
            metadata.put("pages", pages);

            // File info
            FileManagementService.FileRecord record = fileService.getFileRecord(fileId);
            metadata.put("fileName", record.fileName());
            metadata.put("fileSize", record.size());
            metadata.put("fileSizeFormatted", formatFileSize(record.size()));

            log.info("Extracted metadata from PDF {}", fileId);
            return metadata;
        } catch (IOException e) {
            throw new PdfProcessingException("Failed to extract metadata: " + e.getMessage(), e);
        }
    }

    private void putIfNotNull(Map<String, String> map, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            map.put(key, value);
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
