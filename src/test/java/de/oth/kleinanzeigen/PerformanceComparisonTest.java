package de.oth.kleinanzeigen;

import com.mongodb.BasicDBObject;
import com.mongodb.ExplainVerbosity;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

@SpringBootTest
public class PerformanceComparisonTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    public void compareQueryPlans() {
        String categoryToSearch = "Elektronik";
        System.out.println("\n================ PERFORMANCE COMPARISON ================");
        System.out.println("Query: Find all ads in category '" + categoryToSearch + "' AND Sort by 'publishedAt' DESC");

        // 1. Analyze Collection Query
        System.out.println("\n--- 1. COLLECTION (ads) ---");
        Document collectionExplain = mongoTemplate.getDb().getCollection("ads")
                .find(new BasicDBObject("category", categoryToSearch)).sort(new BasicDBObject("publishedAt", -1))
                .explain(ExplainVerbosity.EXECUTION_STATS);

        printStats(collectionExplain);

        // 2. Analyze View Query
        System.out.println("\n--- 2. VIEW (ads_view) ---");
        Document viewExplain = mongoTemplate.getDb().getCollection("ads_view")
                .find(new BasicDBObject("category", categoryToSearch)).sort(new BasicDBObject("publishedAt", -1))
                .explain(ExplainVerbosity.EXECUTION_STATS);

        printStats(viewExplain);
        System.out.println("========================================================\n");
    }

    private void printStats(Document explain) {
        System.out.println("--- Explain Result (JSON Snippet) ---");
        String json = explain.toJson();
        // Print first 1000 chars to avoid flooding console, but enough to see the structure
        System.out.println(json.length() > 1500 ? json.substring(0, 1500) + "..." : json);

        try {
            // Try to extract time if available in standard location
            if (explain.containsKey("executionStats")) {
                Document stats = (Document) explain.get("executionStats");
                System.out.println(">> Execution Time: " + stats.get("executionTimeMillis") + " ms");
                System.out.println(">> Keys Examined:  " + stats.get("totalKeysExamined"));
                System.out.println(">> Docs Examined:  " + stats.get("totalDocsExamined"));
            } else {
                System.out.println(
                        ">> Note: 'executionStats' not found at top level (typical for View/Aggregation explain).");
            }
        } catch (Exception e) {
            System.out.println(">> Could not parse stats details: " + e.getMessage());
        }
    }

    /**
     * Query Optimization:
     *       Schauen Sie in den Block unter "$cursor". Dort sehen Sie den Eintrag "filter": { "category": { "$eq": "Elektronik" } }.
     *        * Bedeutung: MongoDB hat erkannt, dass Sie nur "Elektronik" wollen. Es hat diesen Filter aus der View-Abfrage herausgenommen und direkt auf die darunterliegende Collection angewendet.
     *        * Ohne diese Optimierung: Würde der Filter erst in einer späteren "Stage" stehen, müsste MongoDB für alle 21 Anzeigen die Kommentare zählen und dann 18 davon wegwerfen. Das wäre langsam.
     */
}
