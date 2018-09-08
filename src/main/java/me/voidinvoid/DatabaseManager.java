package me.voidinvoid;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.dv8tion.jda.core.entities.User;
import org.sqlite.SQLiteException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;

public class DatabaseManager {

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

    /*
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss.SSSxxxxx");

    @Deprecated
    public static Connection connect_old() {
        if (!RadioConfig.config.useDatabase || RadioConfig.config.locations.database == null) return null;

        try {
            String url = "jdbc:sqlite:" + RadioConfig.config.locations.database;
            return DriverManager.getConnection(url);

        } catch (SQLException e) {
            System.out.println("Failed to connect to currency database");
            System.out.println(e.getMessage());

            return null;
        }
    }

    @Deprecated
    public static boolean addCredit_old(Map<User, Integer> users) {
        Connection conn = null;
        for (int i = 0; i < 5; i++) { //5 retries
            try {
                conn = connect_old();
                if (conn == null) return false;

                String sql = "INSERT OR REPLACE INTO ServerUsers (Id, Coins, DateAdded, DateUpdated, Username) " +
                        "VALUES (?, COALESCE((SELECT Coins FROM ServerUsers WHERE Id = ?), 0) + ?, COALESCE((SELECT DateAdded FROM ServerUsers WHERE Id = ?), ?), ?, ?);";
                //            id ^           ^^^ add to existing Coins ^^^             ^id      ^ coins          ^^^ keep existing DateAdded ^^^       id ^   ^now^  ^ username
                //               1                                                     2        3                                                         4   5   6  7

                String now = DATE_FORMAT.format(OffsetDateTime.now());

                PreparedStatement s = conn.prepareStatement(sql);

                for (User user : users.keySet()) {
                    int amount = users.get(user);
                    long id = user.getIdLong();

                    s.setLong(1, id);
                    s.setLong(2, id);
                    s.setInt(3, amount);
                    s.setLong(4, id);
                    s.setString(5, now);
                    s.setString(6, now);
                    s.setString(7, user.getName());

                    s.addBatch();
                }

                s.executeBatch();
                conn.close();

                System.out.println("DB updated successfully");
                return true;
            } catch (Exception e) {
                e.printStackTrace();

                if (!(e instanceof SQLiteException)) return false;

                System.out.println("DB WRITE FAILED - RETRYING IN 300ms (attempt " + i + ")");
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                Thread.sleep(300);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }*/
}
