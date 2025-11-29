package de.oth.kleinanzeigen.dto;

import java.util.List;

public class ApiSearchResult {
    private String portal;
    private List<ApiAdResult> results;

    public ApiSearchResult() {
    }

    public ApiSearchResult(String portal, List<ApiAdResult> results) {
        this.portal = portal;
        this.results = results;
    }

    public String getPortal() {
        return portal;
    }

    public void setPortal(String portal) {
        this.portal = portal;
    }

    public List<ApiAdResult> getResults() {
        return results;
    }

    public void setResults(List<ApiAdResult> results) {
        this.results = results;
    }
}
