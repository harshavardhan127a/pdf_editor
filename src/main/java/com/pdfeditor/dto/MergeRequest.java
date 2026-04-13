package com.pdfeditor.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class MergeRequest {

    @NotEmpty(message = "At least two file IDs are required to merge")
    private List<String> fileIds;

    private String outputFileName = "merged.pdf";

    public MergeRequest() {}

    public List<String> getFileIds() { return fileIds; }
    public void setFileIds(List<String> fileIds) { this.fileIds = fileIds; }
    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }
}
