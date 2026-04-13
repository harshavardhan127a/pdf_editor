package com.pdfeditor.service;

import com.pdfeditor.dto.TextInsertRequest;
import com.pdfeditor.exception.PdfProcessingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

@Service
public class PdfEditingService {

    private static final Logger log = LoggerFactory.getLogger(PdfEditingService.class);

    private final FileManagementService fileService;

    public PdfEditingService(FileManagementService fileService) {
        this.fileService = fileService;
    }

    /**
     * Insert text at a specific position on a page.
     */
    public byte[] insertText(String fileId, TextInsertRequest request) {
        Path filePath = fileService.getFilePath(fileId);

        try (PDDocument doc = Loader.loadPDF(filePath.toFile())) {
            if (request.getPageNumber() >= doc.getNumberOfPages()) {
                throw new IllegalArgumentException("Page number exceeds document pages");
            }

            PDPage page = doc.getPage(request.getPageNumber());

            // Resolve font
            Standard14Fonts.FontName fontEnum = resolveFont(request.getFontName());
            PDType1Font font = new PDType1Font(fontEnum);

            // Parse color
            Color color = parseColor(request.getColor());

            try (PDPageContentStream contentStream = new PDPageContentStream(
                    doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                contentStream.beginText();
                contentStream.setFont(font, request.getFontSize());
                contentStream.setNonStrokingColor(color);
                contentStream.newLineAtOffset(request.getX(), request.getY());

                // Handle multi-line text
                String[] lines = request.getText().split("\n");
                for (int i = 0; i < lines.length; i++) {
                    contentStream.showText(lines[i]);
                    if (i < lines.length - 1) {
                        contentStream.newLineAtOffset(0, -(request.getFontSize() + 4));
                    }
                }

                contentStream.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            log.info("Inserted text on page {} of PDF {}", request.getPageNumber(), fileId);
            return out.toByteArray();
        } catch (IOException e) {
            throw new PdfProcessingException("Failed to insert text: " + e.getMessage(), e);
        }
    }

    /**
     * Insert an image at a specific position on a page.
     */
    public byte[] insertImage(String fileId, MultipartFile imageFile, int pageNumber,
                              float x, float y, float width, float height) {
        Path filePath = fileService.getFilePath(fileId);

        try (PDDocument doc = Loader.loadPDF(filePath.toFile())) {
            if (pageNumber >= doc.getNumberOfPages()) {
                throw new IllegalArgumentException("Page number exceeds document pages");
            }

            PDPage page = doc.getPage(pageNumber);
            PDImageXObject image = PDImageXObject.createFromByteArray(
                    doc, imageFile.getBytes(), imageFile.getOriginalFilename());

            // Auto-size if width/height not provided
            float imgWidth = width > 0 ? width : image.getWidth();
            float imgHeight = height > 0 ? height : image.getHeight();

            try (PDPageContentStream contentStream = new PDPageContentStream(
                    doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                contentStream.drawImage(image, x, y, imgWidth, imgHeight);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            log.info("Inserted image on page {} of PDF {}", pageNumber, fileId);
            return out.toByteArray();
        } catch (IOException e) {
            throw new PdfProcessingException("Failed to insert image: " + e.getMessage(), e);
        }
    }

    private Standard14Fonts.FontName resolveFont(String fontName) {
        return switch (fontName.toLowerCase()) {
            case "courier" -> Standard14Fonts.FontName.COURIER;
            case "courier-bold", "courier_bold" -> Standard14Fonts.FontName.COURIER_BOLD;
            case "times", "times-roman", "times_roman" -> Standard14Fonts.FontName.TIMES_ROMAN;
            case "times-bold", "times_bold" -> Standard14Fonts.FontName.TIMES_BOLD;
            case "helvetica-bold", "helvetica_bold" -> Standard14Fonts.FontName.HELVETICA_BOLD;
            default -> Standard14Fonts.FontName.HELVETICA;
        };
    }

    private Color parseColor(String hex) {
        try {
            if (hex.startsWith("#")) hex = hex.substring(1);
            return new Color(
                    Integer.parseInt(hex.substring(0, 2), 16),
                    Integer.parseInt(hex.substring(2, 4), 16),
                    Integer.parseInt(hex.substring(4, 6), 16)
            );
        } catch (Exception e) {
            return Color.BLACK;
        }
    }
}
