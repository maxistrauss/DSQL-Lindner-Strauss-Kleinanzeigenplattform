package de.oth.kleinanzeigen.dto;

public class TopSearchTerm {
    private String term;
    private long count;

    public TopSearchTerm() {
    }

    public TopSearchTerm(String term, long count) {
        this.term = term;
        this.count = count;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
