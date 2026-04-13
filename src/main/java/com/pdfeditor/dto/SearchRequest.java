package com.pdfeditor.dto;

import jakarta.validation.constraints.NotBlank;

public class SearchRequest {

    @NotBlank(message = "Search query is required")
    private String query;

    private boolean caseSensitive = false;

    public SearchRequest() {}

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public boolean isCaseSensitive() { return caseSensitive; }
    public void setCaseSensitive(boolean caseSensitive) { this.caseSensitive = caseSensitive; }
}
