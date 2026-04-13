package com.pdfeditor.dto;

import java.util.List;

public class SearchResult {

    private String query;
    private int totalMatches;
    private List<PageMatch> matches;

    public SearchResult() {}

    public SearchResult(String query, int totalMatches, List<PageMatch> matches) {
        this.query = query;
        this.totalMatches = totalMatches;
        this.matches = matches;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public int getTotalMatches() { return totalMatches; }
    public void setTotalMatches(int totalMatches) { this.totalMatches = totalMatches; }
    public List<PageMatch> getMatches() { return matches; }
    public void setMatches(List<PageMatch> matches) { this.matches = matches; }

    public static class PageMatch {
        private int pageNumber;
        private int count;
        private String contextSnippet;

        public PageMatch() {}

        public PageMatch(int pageNumber, int count, String contextSnippet) {
            this.pageNumber = pageNumber;
            this.count = count;
            this.contextSnippet = contextSnippet;
        }

        public int getPageNumber() { return pageNumber; }
        public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public String getContextSnippet() { return contextSnippet; }
        public void setContextSnippet(String contextSnippet) { this.contextSnippet = contextSnippet; }
    }
}
