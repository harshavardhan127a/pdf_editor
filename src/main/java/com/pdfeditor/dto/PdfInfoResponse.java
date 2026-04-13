package com.pdfeditor.dto;

import java.util.List;
import java.util.Map;

public class PdfInfoResponse {
    private String fileId;
    private String fileName;
    private long fileSize;
    private int pageCount;
    private double width;
    private double height;
    private boolean encrypted;
    private Map<String, String> metadata;

    public PdfInfoResponse() {}

    public PdfInfoResponse(String fileId, String fileName, long fileSize, int pageCount,
                           double width, double height, boolean encrypted, Map<String, String> metadata) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.pageCount = pageCount;
        this.width = width;
        this.height = height;
        this.encrypted = encrypted;
        this.metadata = metadata;
    }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public int getPageCount() { return pageCount; }
    public void setPageCount(int pageCount) { this.pageCount = pageCount; }
    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = width; }
    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }
    public boolean isEncrypted() { return encrypted; }
    public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}
