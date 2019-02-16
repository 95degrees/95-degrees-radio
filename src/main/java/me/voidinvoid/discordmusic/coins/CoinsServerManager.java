package me.voidinvoid.discordmusic.coins;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.config.RadioConfig;
import net.dv8tion.jda.core.entities.User;
import org.bson.Document;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;

public class CoinsServerManager { //todo convert to radioservice

    private static URL UPDATE_URL;

    static {
        try {
            UPDATE_URL = new URL(RadioConfig.config.locations.coinUpdates);
        } catch (MalformedURLException e) {
            System.out.println("ERROR: COINS UPDATE URL IS INVALID");
            e.printStackTrace();
        }
    }

    public static boolean addCredit(User user, int coins) {
        return addCredit(Collections.singletonMap(user, coins));
    }

    public static boolean addCredit(Map<User, Integer> users) {
        try {
            long timestamp = System.currentTimeMillis();
            JsonArray creditArray = new JsonArray();
            DatabaseManager db = Radio.getInstance().getService(DatabaseManager.class);

            users.forEach((u, c) -> {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", u.getIdLong());
                obj.addProperty("username", u.getName());
                obj.addProperty("coin_gain", c);
                obj.addProperty("timestamp", timestamp);
                creditArray.add(obj);

                if (db != null) {
                    db.findOrCreateUser(u, true);
                    db.getCollection("users").updateOne(eq("_id", u.getId()), new Document("$inc", new Document("total_earned_coins", c))); //todo more efficient?
                }
            });

            JsonObject root = new JsonObject();
            root.add("updates", creditArray);

            URL url = UPDATE_URL;
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.addRequestProperty("Content-Type", "application/json");
            conn.getOutputStream().write(root.toString().getBytes(StandardCharsets.UTF_8));

            int status = conn.getResponseCode();
            if (status != 200) {
                System.out.println("UPDATING COINS ERROR: response code " + status);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}
