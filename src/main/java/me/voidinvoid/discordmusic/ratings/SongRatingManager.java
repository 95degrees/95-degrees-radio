package me.voidinvoid.discordmusic.ratings;

import com.mongodb.client.MongoCollection;
import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.levelling.Achievement;
import me.voidinvoid.discordmusic.levelling.AchievementManager;
import me.voidinvoid.discordmusic.songs.database.DatabaseSong;
import me.voidinvoid.discordmusic.utils.Service;
import net.dv8tion.jda.core.entities.User;
import org.bson.Document;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
public class SongRatingManager implements RadioService {

    private Map<String, Long> lastRatingAttempt = new HashMap<>();

    public boolean rateSong(User user, DatabaseSong song, Rating rating, boolean force) {
        if (!force && (System.currentTimeMillis() - lastRatingAttempt.getOrDefault(user.getId(), 0L)) < 10000L) {
            return false;
        }

        lastRatingAttempt.put(user.getId(), System.currentTimeMillis());

        DatabaseManager db = Radio.getInstance().getService(DatabaseManager.class);
        MongoCollection<Document> ratings = db.getCollection("ratings");

        var am = Service.of(AchievementManager.class);
        am.rewardAchievement(user, Achievement.RATE_SONG);

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

        ratings.updateOne(eq("song", song.getFileName()), new Document("$pull", rt));
        ratings.updateOne(eq("song", song.getFileName()), new Document("$addToSet", new Document("ratings." + rating, user.getId()))).getModifiedCount();

        return true;
    }
}
