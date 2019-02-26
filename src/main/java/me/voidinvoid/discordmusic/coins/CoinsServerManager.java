package me.voidinvoid.discordmusic.coins;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.stats.Statistic;
import me.voidinvoid.discordmusic.stats.UserStatisticsManager;
import me.voidinvoid.discordmusic.utils.Service;
import net.dv8tion.jda.core.entities.User;
import org.bson.Document;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;

public class CoinsServerManager implements RadioService {

    private URI UPDATE_URL;
    private HttpClient client;

    @Override
    public void onLoad() {
        try {
            UPDATE_URL = new URI(RadioConfig.config.locations.coinUpdates);
        } catch (Exception e) {
            warn("Coins update URL is invalid!");
            e.printStackTrace();
        }

        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    public void addCredit(User user, int coins) {
        addCredit(Collections.singletonMap(user, coins));
    }

    public void addCredit(Map<User, Integer> users) {
        try {
            long timestamp = System.currentTimeMillis();
            JsonArray creditArray = new JsonArray();

            var stats = Service.of(UserStatisticsManager.class);

            users.forEach((u, c) -> {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", u.getIdLong());
                obj.addProperty("username", u.getName());
                obj.addProperty("coin_gain", c);
                obj.addProperty("timestamp", timestamp);
                creditArray.add(obj);

                if (stats != null) {
                    stats.addStatistic(u, Statistic.COINS_EARNED, c);
                }
            });

            JsonObject root = new JsonObject();
            root.add("updates", creditArray);

            HttpRequest req = HttpRequest.newBuilder(UPDATE_URL)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(root.toString())).build();

            client.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(r -> {
               if (r.statusCode() != 200) {
                   warn("Error updating coins error: status code " + r.statusCode());
               }
            });

            /*URL url = UPDATE_URL;
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(4000);
            conn.addRequestProperty("Content-Type", "application/json");
            conn.getOutputStream().write(root.toString().getBytes(StandardCharsets.UTF_8));

            int status = conn.getResponseCode();
            if (status != 200) {
                log("UPDATING COINS ERROR: response code " + status);
                return false;
            }*/
        } catch (Exception e) {
            log("ERROR UPDATING COINS");
            e.printStackTrace();
        }
    }
}
