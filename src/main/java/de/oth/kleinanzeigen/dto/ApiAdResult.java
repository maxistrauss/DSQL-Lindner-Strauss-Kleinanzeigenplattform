package de.oth.kleinanzeigen.dto;

import java.math.BigDecimal;

public class ApiAdResult {
    private String article;
    private BigDecimal price;
    private String url;

    public ApiAdResult() {
    }

    public ApiAdResult(String article, BigDecimal price, String url) {
        this.article = article;
        this.price = price;
        this.url = url;
    }

    public String getArticle() {
        return article;
    }

    public void setArticle(String article) {
        this.article = article;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
