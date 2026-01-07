package de.oth.kleinanzeigen.model.elastic;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "search_terms")
public class SearchTerm {

    @Id
    private String id; // The search term itself

    @Field(type = FieldType.Text)
    private String term;

    @Field(type = FieldType.Integer)
    private int count;

    public SearchTerm() {
    }

    public SearchTerm(String term, int count) {
        this.id = term.toLowerCase().trim();
        this.term = term;
        this.count = count;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
