package com.pdfeditor.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class ReorderRequest {

    /**
     * New page order as a list of page indices (0-indexed).
     * Example: [2, 0, 1] moves page 3 to first, page 1 to second, page 2 to third.
     */
    @NotEmpty(message = "Page order list cannot be empty")
    private List<Integer> pageOrder;

    public ReorderRequest() {}

    public List<Integer> getPageOrder() { return pageOrder; }
    public void setPageOrder(List<Integer> pageOrder) { this.pageOrder = pageOrder; }
}
