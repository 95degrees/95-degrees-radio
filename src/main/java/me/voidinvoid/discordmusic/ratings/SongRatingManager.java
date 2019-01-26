package me.voidinvoid.discordmusic.ratings;

import com.mongodb.client.MongoCollection;
import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.songs.database.DatabaseSong;
import net.dv8tion.jda.core.entities.User;
import org.bson.Document;

import java.util.Collections;

import static com.mongodb.client.model.Filters.eq;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
public class SongRatingManager {

    public boolean rateSong(User user, DatabaseSong song, Rating rating) {
        DatabaseManager db = Radio.getInstance().getService(DatabaseManager.class);
        MongoCollection<Document> ratings = db.getCollection("ratings");

        if (ratings.find(eq("song", song.getFileName())).first() == null) {
            Document d = new Document();
            d.put("song", song.getFileName());
            Document rt = new Document();
            for (Rating r : Rating.values()) {
                rt.put(r.name(), r == rating ? Collections.singletonList(user.getId()) : Collections.emptyList());
            }
            d.put("ratings", rt);
            ratings.insertOne(d);
            return true;
        }

        Document rt = new Document();
        for (Rating r : Rating.values()) {
            rt.put("ratings." + r, user.getId());
        }

        ratings.updateOne(eq("song", song.getFileName()), new Document("$pullAll", rt));
        return ratings.updateOne(eq("song", song.getFileName()), new Document("$addToSet", new Document("ratings." + rating, user.getId()))).getModifiedCount() != 0;
    }
}
