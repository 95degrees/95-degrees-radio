package me.voidinvoid.discordmusic;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.core.entities.User;
import org.bson.Document;

import java.util.Collections;

import static com.mongodb.client.model.Filters.eq;

public class DatabaseManager implements RadioService {

    private MongoClient client;
    private MongoDatabase db;

    public DatabaseManager() {
        client = MongoClients.create();
        db = client.getDatabase("95degrees-radio");
    }

    public MongoClient getClient() {
        return client;
    }

    public MongoCollection<Document> getCollection(String coll) {
        return db.getCollection(coll);
    }

    public Document findOrCreateUser(User user, boolean insertIfEmpty) {
        MongoCollection<Document> users = getCollection("users");
        Document d = users.find(eq(user.getId())).limit(1).first();

        if (d == null) {
            d = new Document("_id", user.getId())
                    .append("subscriptions", Collections.emptyList())
                    .append("ratings", Collections.emptyList())
                    .append("achievements", Collections.emptyList())
                    .append("created", System.currentTimeMillis())
                    .append("total_earned_coins", 0)
                    .append("total_listen_time", 0)
                    .append("total_experience", 0)
                    .append("data_version", 1);

            if (insertIfEmpty) {
                users.insertOne(d);
            }
        }

        return d;
    }

    public Document findOrCreateUser(User user) {
        return findOrCreateUser(user, false);
    }
}
