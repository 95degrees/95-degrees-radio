package me.voidinvoid.karaoke;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

public class LyricsManager {

    static {
        System.setProperty("http.agent", "");
    }

    public static Lyrics findLyricsFor(String youtubeId) {
        try {
            URL url = new URL("https://extension.musixmatch.com/?res=" + generateId(youtubeId));// + "&hl=en-GB&v=" + youtubeId + "&type=track&lang=en&name&kind&fmt=1");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36");
            conn.setRequestMethod("GET");

            int status = conn.getResponseCode();

            if (status != 200) {
                System.out.println("Error fetching lyrics, response code " + status);
                return null;
            }

            String result = new BufferedReader(new InputStreamReader(conn.getInputStream())).lines().collect(Collectors.joining("\n"));

            return new Lyrics(result);

        } catch (Exception e) {
            System.out.println("Error fetching lyrics for " + youtubeId);
            e.printStackTrace();
            return null;
        }
    }

    private static String generateId(String youtubeId) {
        StringBuilder i = new StringBuilder();
        int n = 0;
        int t;
        int r;
        while (n < youtubeId.length()) {
            t = youtubeId.charAt(n) + 13;
            r = (int) Math.floor(Math.random() * 3 + 1);
            i.append(t).append(getRandomBasedOn(r));
            n++;
        }
        return i.toString();
    }

    private static String getRandomBasedOn(int e) {
        StringBuilder r = new StringBuilder();
        String n = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        int t = 0;
        while (t < e) {
            r.append(n.charAt((int) Math.floor(Math.random() * n.length())));
            t++;
        }
        return r.toString();
    }
}