package com.pdfeditor.service;

import com.pdfeditor.exception.PdfProcessingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

@Service
public class ConversionService {

    private static final Logger log = LoggerFactory.getLogger(ConversionService.class);

    private final FileManagementService fileService;

    public ConversionService(FileManagementService fileService) {
        this.fileService = fileService;
    }

    /**
     * Convert PDF to DOCX by extracting text per page.
     */
    public byte[] convertToDocx(String fileId) {
        Path filePath = fileService.getFilePath(fileId);

        try (PDDocument pdfDoc = Loader.loadPDF(filePath.toFile());
             XWPFDocument docxDoc = new XWPFDocument()) {

            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = pdfDoc.getNumberOfPages();

            for (int i = 1; i <= totalPages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String pageText = stripper.getText(pdfDoc);

                // Add page heading
                XWPFParagraph heading = docxDoc.createParagraph();
                heading.setStyle("Heading2");
                XWPFRun headingRun = heading.createRun();
                headingRun.setText("Page " + i);
                headingRun.setBold(true);
                headingRun.setFontSize(14);

                // Add page content
                String[] lines = pageText.split("\n");
                for (String line : lines) {
                    XWPFParagraph para = docxDoc.createParagraph();
                    XWPFRun run = para.createRun();
                    run.setText(line);
                    run.setFontSize(11);
                    run.setFontFamily("Calibri");
                }

                // Add page break between pages (except last)
                if (i < totalPages) {
                    XWPFParagraph pageBreak = docxDoc.createParagraph();
                    pageBreak.setPageBreak(true);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            docxDoc.write(out);
            log.info("Converted PDF {} to DOCX: {} pages", fileId, totalPages);
            return out.toByteArray();
        } catch (IOException e) {
            throw new PdfProcessingException("Failed to convert PDF to DOCX: " + e.getMessage(), e);
        }
    }

    /**
     * Convert PDF to PPTX by creating one slide per page.
     */
    public byte[] convertToPptx(String fileId) {
        Path filePath = fileService.getFilePath(fileId);

        try (PDDocument pdfDoc = Loader.loadPDF(filePath.toFile());
             XMLSlideShow pptx = new XMLSlideShow()) {

            // Set slide dimensions to match PDF page
            PDPage firstPage = pdfDoc.getPage(0);
            PDRectangle mediaBox = firstPage.getMediaBox();
            pptx.setPageSize(new Dimension(
                    (int) (mediaBox.getWidth() * 96 / 72),  // Convert PDF points to pixels
                    (int) (mediaBox.getHeight() * 96 / 72)
            ));

            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = pdfDoc.getNumberOfPages();

            for (int i = 1; i <= totalPages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String pageText = stripper.getText(pdfDoc);

                XSLFSlide slide = pptx.createSlide();

                // Add title text box
                XSLFTextBox titleBox = slide.createTextBox();
                titleBox.setAnchor(new java.awt.Rectangle(40, 20, 600, 50));
                XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
                XSLFTextRun titleRun = titlePara.addNewTextRun();
                titleRun.setText("Page " + i);
                titleRun.setFontSize(18.0);
                titleRun.setBold(true);

                // Add content text box
                XSLFTextBox contentBox = slide.createTextBox();
                contentBox.setAnchor(new java.awt.Rectangle(40, 80,
                        (int) (mediaBox.getWidth() * 96 / 72) - 80,
                        (int) (mediaBox.getHeight() * 96 / 72) - 120));

                String[] lines = pageText.split("\n");
                for (int j = 0; j < lines.length; j++) {
                    XSLFTextParagraph para;
                    if (j == 0) {
                        para = contentBox.getTextParagraphs().get(0);
                    } else {
                        para = contentBox.addNewTextParagraph();
                    }
                    XSLFTextRun run = para.addNewTextRun();
                    run.setText(lines[j]);
                    run.setFontSize(10.0);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pptx.write(out);
            log.info("Converted PDF {} to PPTX: {} slides", fileId, totalPages);
            return out.toByteArray();
        } catch (IOException e) {
            throw new PdfProcessingException("Failed to convert PDF to PPTX: " + e.getMessage(), e);
        }
    }
}
