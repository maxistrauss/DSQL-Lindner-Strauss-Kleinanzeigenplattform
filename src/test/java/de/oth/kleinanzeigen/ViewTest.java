package de.oth.kleinanzeigen;

import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

@SpringBootTest
public class ViewTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    public void checkViewStatus() {
        MongoDatabase db = mongoTemplate.getDb();
        boolean exists = false;
        boolean isView = false;

        System.out.println("--- Checking View Status ---");
        for (Document doc : db.listCollections()) {
            String name = doc.getString("name");
            String type = doc.getString("type");

            if ("ads_view".equals(name)) {
                exists = true;
                System.out.println("Found 'ads_view': " + doc.toJson());
                if ("view".equals(type) || (doc.get("options") != null && ((Document) doc.get("options")).containsKey(
                        "viewOn"))) {
                    isView = true;
                }
            }
        }

        Assertions.assertTrue(exists, "Collection 'ads_view' should exist");
        Assertions.assertTrue(isView, "'ads_view' should be a MongoDB View, not a standard collection");

        long count = mongoTemplate.getCollection("ads_view").countDocuments();
        System.out.println("Count in view: " + count);
        Assertions.assertTrue(count > 0, "View 'ads_view' should contain documents (mirrored from 'ads')");

        Document firstDoc = mongoTemplate.getCollection("ads_view").find().first();
        Assertions.assertNotNull(firstDoc, "Should be able to retrieve a document from the view");
        Assertions.assertTrue(firstDoc.containsKey("numberOfComments"),
                "Document in view should have 'numberOfComments' field");
        System.out.println("First document sample: " + firstDoc.toJson());
    }
}
