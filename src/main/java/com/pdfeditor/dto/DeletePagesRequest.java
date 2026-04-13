package com.pdfeditor.dto;

import java.util.List;

public class DeletePagesRequest {

    private List<Integer> pageNumbers;

    public DeletePagesRequest() {}

    public List<Integer> getPageNumbers() { return pageNumbers; }
    public void setPageNumbers(List<Integer> pageNumbers) { this.pageNumbers = pageNumbers; }
}
