package me.voidinvoid.discordmusic;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;

import java.util.Collections;

import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class DatabaseManager implements RadioService {

    private final String configName;
    private MongoClient client;
    private MongoDatabase db;

    public DatabaseManager(String configName) {
        this.configName = configName;
        CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        client = MongoClients.create(MongoClientSettings.builder().codecRegistry(codecRegistry).build());
        db = client.getDatabase("95degrees-radio");
        db.getCollection("internal").updateOne(eq("_id", configName), new Document("$setOnInsert", new Document("creationDate", System.currentTimeMillis())), new UpdateOptions().upsert(true));
    }

    public Document getInternalDocument() {
        return db.getCollection("internal").find(eq("_id", configName)).first();
    }

    public UpdateResult updateInternalDocument(Bson query) {
        return db.getCollection("internal").updateOne(eq("_id", configName), query);
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
                    .append("stats", new Document())
                    .append("total_earned_coins", 0)
                    .append("total_experience", 0)
                    .append("data_version", 1);

            if (insertIfEmpty) {
                users.insertOne(d);
            }
        }

        return d;
    }

    public Document findOrCreateUser(User user) {
        return findOrCreateUser(user, true);
    }
}
