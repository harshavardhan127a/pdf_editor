package com.pdfeditor.service;

import com.pdfeditor.dto.*;
import com.pdfeditor.exception.PdfProcessingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class PdfProcessingService {

    private static final Logger log = LoggerFactory.getLogger(PdfProcessingService.class);

    private final FileManagementService fileService;

    public PdfProcessingService(FileManagementService fileService) {
        this.fileService = fileService;
    }

    /**
     * Get PDF info (page count, dimensions, metadata).
     */
    public PdfInfoResponse getInfo(String fileId) {
        Path filePath = fileService.getFilePath(fileId);
        FileManagementService.FileRecord record = fileService.getFileRecord(fileId);

        try (PDDocument doc = Loader.loadPDF(filePath.toFile())) {
            PDPage firstPage = doc.getPage(0);
            PDRectangle mediaBox = firstPage.getMediaBox();

            Map<String, String> metadata = new LinkedHashMap<>();
            var docInfo = doc.getDocumentInformation();
            if (docInfo != null) {
                if (docInfo.getTitle() != null) metadata.put("title", docInfo.getTitle());
                if (docInfo.getAuthor() != null) metadata.put("author", docInfo.getAuthor());
                if (docInfo.getSubject() != null) metadata.put("subject", docInfo.getSubject());
                if (docInfo.getCreator() != null) metadata.put("creator", docInfo.getCreator());
                if (docInfo.getProducer() != null) metadata.put("producer", docInfo.getProducer());
                if (docInfo.getCreationDate() != null)
                    metadata.put("creationDate", docInfo.getCreationDate().getTime().toString());
                if (docInfo.getModificationDate() != null)
                    metadata.put("modificationDate", docInfo.getModificationDate().getTime().toString());
            }

            return new PdfInfoResponse(
                    fileId,
                    record.fileName(),
                    record.size(),
                    doc.getNumberOfPages(),
                    mediaBox.getWidth(),
                    mediaBox.getHeight(),
                    doc.isEncrypted(),
                    metadata
            );
        } catch (IOException e) {
            throw new PdfProcessingException("Failed to read PDF info", e);
        }
    }

    /**
     * Merge multiple PDFs into one.
     */
    public byte[] merge(List<String> fileIds) {
        if (fileIds == null || fileIds.size() < 2) {
            throw new IllegalArgumentException("At least two files are required for merging");
        }

        try {
            PDFMergerUtility merger = new PDFMergerUtility();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            merger.setDestinationStream(outputStream);

            for (String fileId : fileIds) {
                Path filePath = fileService.getFilePath(fileId);
                merger.addSource(filePath.toFile());
            }

            merger.mergeDocuments(null);
            log.info("Merged {} PDFs successfully", fileIds.size());
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new PdfProcessingException("Failed to merge PDFs", e);
        }
    }

    /**
     * Split a PDF by page ranges.
     */
    public List<byte[]> split(String fileId, List<int[]> pageRanges) {
        Path filePath = fileService.getFilePath(fileId);

        try (PDDocument doc = Loader.loadPDF(filePath.toFile())) {
            List<byte[]> results = new ArrayList<>();

            if (pageRanges == null || pageRanges.isEmpty()) {
                // Split into individual pages
                Splitter splitter = new Splitter();
                List<PDDocument> pages = splitter.split(doc);
                for (PDDocument page : pages) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    page.save(out);
                    results.add(out.toByteArray());
                    page.close();
                }
            } else {
                // Split by specified ranges
                for (int[] range : pageRanges) {
                    if (range.length != 2 || range[0] < 1 || range[1] > doc.getNumberOfPages() || range[0] > range[1]) {
                        throw new IllegalArgumentException("Invalid page range: " + Arrays.toString(range));
                    }

                    try (PDDocument newDoc = new PDDocument()) {
                        for (int i = range[0] - 1; i < range[1]; i++) {
                            newDoc.addPage(doc.getPage(i));
                        }
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        newDoc.save(out);
                        results.add(out.toByteArray());
                    }
                }
            }

            log.info("Split PDF {} into {} parts", fileId, results.size());
            return results;
        } catch (IOException e) {
            throw new PdfProcessingException("Failed to split PDF", e);
        }
    }

    /**
     * Reorder pages in a PDF.
     */
    public byte[] reorderPages(String fileId, List<Integer> pageOrder) {
        Path filePath = fileService.getFilePath(fileId);

        try (PDDocument doc = Loader.loadPDF(filePath.toFile())) {
            int totalPages = doc.getNumberOfPages();

            // Validate page order
            if (pageOrder.size() != totalPages) {
                throw new IllegalArgumentException(
                        "Page order must contain exactly " + totalPages + " indices, got " + pageOrder.size());
            }

            Set<Integer> seen = new HashSet<>(pageOrder);
            if (seen.size() != totalPages || pageOrder.stream().anyMatch(i -> i < 0 || i >= totalPages)) {
                throw new IllegalArgumentException("Invalid page order: must be a permutation of 0 to " + (totalPages - 1));
            }

            // Collect pages in new order
            List<PDPage> pages = new ArrayList<>();
            for (int index : pageOrder) {
                pages.add(doc.getPage(index));
            }

            // Create new document with reordered pages
            try (PDDocument newDoc = new PDDocument()) {
                for (PDPage page : pages) {
                    newDoc.importPage(page);
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                newDoc.save(out);
                log.info("Reordered pages in PDF {}", fileId);
                return out.toByteArray();
            }
        } catch (IOException e) {
            throw new PdfProcessingException("Failed to reorder pages", e);
        }
    }

    /**
     * Delete specific pages from a PDF.
     */
    public byte[] deletePages(String fileId, List<Integer> pageNumbers) {
        Path filePath = fileService.getFilePath(fileId);

        try (PDDocument doc = Loader.loadPDF(filePath.toFile())) {
            int totalPages = doc.getNumberOfPages();

            // Sort in reverse to avoid index shifting
            List<Integer> sorted = new ArrayList<>(pageNumbers);
            sorted.sort(Collections.reverseOrder());

            for (int pageNum : sorted) {
                if (pageNum < 0 || pageNum >= totalPages) {
                    throw new IllegalArgumentException("Invalid page number: " + pageNum);
                }
                doc.removePage(pageNum);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            log.info("Deleted {} pages from PDF {}", pageNumbers.size(), fileId);
            return out.toByteArray();
        } catch (IOException e) {
            throw new PdfProcessingException("Failed to delete pages", e);
        }
    }

    /**
     * Add blank pages to a PDF.
     */
    public byte[] addBlankPage(String fileId, int afterPageIndex) {
        Path filePath = fileService.getFilePath(fileId);

        try (PDDocument doc = Loader.loadPDF(filePath.toFile())) {
            PDPage blankPage = new PDPage(PDRectangle.A4);

            if (afterPageIndex < 0) {
                // Insert at the beginning
                doc.getPages().insertBefore(blankPage, doc.getPage(0));
            } else if (afterPageIndex >= doc.getNumberOfPages()) {
                doc.addPage(blankPage);
            } else {
                doc.getPages().insertAfter(blankPage, doc.getPage(afterPageIndex));
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            log.info("Added blank page to PDF {} at position {}", fileId, afterPageIndex);
            return out.toByteArray();
        } catch (IOException e) {
            throw new PdfProcessingException("Failed to add blank page", e);
        }
    }

    /**
     * Extract all text from a PDF.
     */
    public String extractText(String fileId) {
        Path filePath = fileService.getFilePath(fileId);

        try (PDDocument doc = Loader.loadPDF(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            log.info("Extracted text from PDF {}: {} chars", fileId, text.length());
            return text;
        } catch (IOException e) {
            throw new PdfProcessingException("Failed to extract text from PDF", e);
        }
    }

    /**
     * Search for text within a PDF.
     */
    public SearchResult searchText(String fileId, String query, boolean caseSensitive) {
        Path filePath = fileService.getFilePath(fileId);

        try (PDDocument doc = Loader.loadPDF(filePath.toFile())) {
            List<SearchResult.PageMatch> matches = new ArrayList<>();
            int totalMatches = 0;

            PDFTextStripper stripper = new PDFTextStripper();

            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String pageText = stripper.getText(doc);

                String searchText = caseSensitive ? pageText : pageText.toLowerCase();
                String searchQuery = caseSensitive ? query : query.toLowerCase();

                int count = 0;
                int idx = 0;
                String snippet = "";

                while ((idx = searchText.indexOf(searchQuery, idx)) != -1) {
                    count++;
                    if (snippet.isEmpty()) {
                        // Get context around first match
                        int start = Math.max(0, idx - 40);
                        int end = Math.min(searchText.length(), idx + searchQuery.length() + 40);
                        snippet = "..." + pageText.substring(start, end).trim() + "...";
                    }
                    idx += searchQuery.length();
                }

                if (count > 0) {
                    matches.add(new SearchResult.PageMatch(i, count, snippet));
                    totalMatches += count;
                }
            }

            log.info("Search '{}' in PDF {}: {} matches across {} pages", query, fileId, totalMatches, matches.size());
            return new SearchResult(query, totalMatches, matches);
        } catch (IOException e) {
            throw new PdfProcessingException("Failed to search PDF: " + e.getMessage(), e);
        }
    }
}
