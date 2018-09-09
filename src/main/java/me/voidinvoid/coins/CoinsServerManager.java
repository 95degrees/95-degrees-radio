package me.voidinvoid.coins;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.voidinvoid.config.RadioConfig;
import net.dv8tion.jda.core.entities.User;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

public class CoinsServerManager {

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
            users.forEach((u, c) -> {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", u.getIdLong());
                obj.addProperty("username", u.getName());
                obj.addProperty("coin_gain", Math.min(1000, c)); //coin cap of 1000
                obj.addProperty("timestamp", timestamp);
                creditArray.add(obj);
            });

            JsonObject root = new JsonObject();
            root.add("updates", creditArray);

            URL url = UPDATE_URL;
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.addRequestProperty("Content-Type", "application/json");
            conn.getOutputStream().write(root.toString().getBytes("UTF8"));

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
