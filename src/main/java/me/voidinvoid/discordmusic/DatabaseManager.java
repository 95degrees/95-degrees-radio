package me.voidinvoid.discordmusic;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class DatabaseManager extends RadioService {

    private MongoClient client;
    private MongoDatabase db;

    public DatabaseManager() {
        client = MongoClients.create();
        db = client.getDatabase("discord-radio");
    }

    public MongoClient getClient() {
        return client;
    }

    public MongoCollection<Document> getCollection(String coll) {
        return db.getCollection(coll);
    }
}
