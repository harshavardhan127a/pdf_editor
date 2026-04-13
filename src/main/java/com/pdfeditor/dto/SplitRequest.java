package com.pdfeditor.dto;

import java.util.List;

public class SplitRequest {

    /**
     * List of page ranges to split. Each range is [start, end] (1-indexed, inclusive).
     * Example: [[1,3],[4,6]] splits into two documents: pages 1-3 and pages 4-6.
     * If empty, splits into individual pages.
     */
    private List<int[]> pageRanges;

    public SplitRequest() {}

    public List<int[]> getPageRanges() { return pageRanges; }
    public void setPageRanges(List<int[]> pageRanges) { this.pageRanges = pageRanges; }
}
