package com.pdfeditor.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class PdfSecurityUtil {

    private static final Logger log = LoggerFactory.getLogger(PdfSecurityUtil.class);

    private PdfSecurityUtil() {}

    /**
     * Protects a PDF with a password.
     */
    public static void protectPdf(PDDocument document, String ownerPassword, String userPassword) throws IOException {
        AccessPermission ap = new AccessPermission();
        ap.setCanPrint(true);
        ap.setCanExtractContent(false);
        ap.setCanModify(false);

        StandardProtectionPolicy policy = new StandardProtectionPolicy(ownerPassword, userPassword, ap);
        policy.setEncryptionKeyLength(128);

        document.protect(policy);
        log.info("PDF protected with password encryption");
    }

    /**
     * Checks if a PDF document is encrypted.
     */
    public static boolean isEncrypted(PDDocument document) {
        return document.isEncrypted();
    }
}
