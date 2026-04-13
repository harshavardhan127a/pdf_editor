package com.pdfeditor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class BatchProcessingService {

    private static final Logger log = LoggerFactory.getLogger(BatchProcessingService.class);

    private final PdfProcessingService pdfProcessingService;
    private final ConversionService conversionService;
    private final FileManagementService fileService;

    public BatchProcessingService(PdfProcessingService pdfProcessingService,
                                  ConversionService conversionService,
                                  FileManagementService fileService) {
        this.pdfProcessingService = pdfProcessingService;
        this.conversionService = conversionService;
        this.fileService = fileService;
    }

    /**
     * Batch merge: merge all provided files into one PDF.
     */
    public byte[] batchMerge(List<String> fileIds) {
        return pdfProcessingService.merge(fileIds);
    }

    /**
     * Batch convert: convert multiple PDFs to a specified format, return as ZIP.
     */
    public byte[] batchConvert(List<String> fileIds, String targetFormat) throws IOException {
        ByteArrayOutputStream zipOut = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(zipOut)) {
            int index = 1;
            for (String fileId : fileIds) {
                byte[] converted;
                String extension;

                switch (targetFormat.toLowerCase()) {
                    case "docx":
                        converted = conversionService.convertToDocx(fileId);
                        extension = ".docx";
                        break;
                    case "pptx":
                        converted = conversionService.convertToPptx(fileId);
                        extension = ".pptx";
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported target format: " + targetFormat);
                }

                FileManagementService.FileRecord record = fileService.getFileRecord(fileId);
                String baseName = record.fileName().replaceAll("\\.pdf$", "");
                String entryName = baseName + extension;

                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                zos.write(converted);
                zos.closeEntry();

                log.info("Batch converted file {} ({}/{})", fileId, index, fileIds.size());
                index++;
            }
        }

        log.info("Batch conversion complete: {} files to {}", fileIds.size(), targetFormat);
        return zipOut.toByteArray();
    }
}
