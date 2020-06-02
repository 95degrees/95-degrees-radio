package me.voidinvoid.discordmusic.lyrics;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoCollection;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.events.SongEventListener;
import me.voidinvoid.discordmusic.songs.NetworkSong;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.SongType;
import me.voidinvoid.discordmusic.songs.database.DatabaseSong;
import me.voidinvoid.discordmusic.utils.Service;
import me.voidinvoid.discordmusic.utils.Songs;
import org.bson.Document;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static com.mongodb.client.model.Filters.eq;

public class LiveLyricsManager implements RadioService, SongEventListener {

    private DatabaseManager databaseManager;
    private MongoCollection<Document> lyrics;
    private boolean enabled;
    private LiveLyrics activeSongLyrics;

    @Override
    public void onLoad() {
        databaseManager = Service.of(DatabaseManager.class);

        lyrics = databaseManager.getCollection("lyrics");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LiveLyrics getActiveSongLyrics() {
        return activeSongLyrics;
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        if (song.getType() != SongType.SONG) return;

        var name = song.getInternalName();

        log("Attempting to fetch lyrics for " + name + "...");

        fetchLyrics(song, track).whenComplete((l, e) -> {
            var ct = Radio.getInstance().getOrchestrator().getPlayer().getPlayingTrack();

            if (l != null && track.equals(ct)) {
                activeSongLyrics = l;
                log("ACTIVE SONG LYRICS: " + activeSongLyrics);
            }
        });
    }

    @Override
    public void onSongEnd(Song song, AudioTrack track) {
        activeSongLyrics = null;
    }

    private CompletableFuture<LiveLyrics> fetchLyrics(Song song, AudioTrack track) {

        var future = new CompletableFuture<LiveLyrics>();

        var existingLyrics = lyrics.find(eq(song.getInternalName())).first();
        if (existingLyrics != null) {
            log("Lyrics already exist for " + song.getInternalName());

            future.complete(new LiveLyrics(existingLyrics.getString("subtitles")));
            return future;
        }

        if (!enabled) {
            future.complete(null);
            return future;
        }

        var title = track.getInfo().title;
        var artist = track.getInfo().author;

        if (song instanceof NetworkSong) {
            var ns = (NetworkSong) song;

            title = Songs.deyoutubeifySong(title);

            artist = artist.toLowerCase()
                    .replaceAll("vevo", "");

        } else if (song instanceof DatabaseSong) {
            var ds = (DatabaseSong) song;

            title = ds.getTitle();
            artist = ds.getArtist();
        }

        var cm = new CookieManager();
        cm.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        var client = HttpClient.newBuilder().cookieHandler(cm).followRedirects(HttpClient.Redirect.NORMAL).build();

        var request = HttpRequest.newBuilder(URI.create("https://apic-desktop.musixmatch.com/ws/1.1/macro.subtitles.get" +
                "?format=json" +
                "&namespace=lyrics_synched" +
                "&q_artist=" + URLEncoder.encode(artist, StandardCharsets.UTF_8) +
                "&q_duration=" + track.getDuration() / 1000 +
                "&q_track=" + URLEncoder.encode(title, StandardCharsets.UTF_8) +
                "&user_language=en" +
                "&app_id=web-desktop-app-v1.0" +
                "&usertoken=200531ee9b232f7d528447be1e137f066f836100534621b21c3027"))
                .header("cache-control", "no-cache")
                .header("accept", "*/*")
                .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Musixmatch/3.14.4564-master.20200505002 Chrome/78.0.3904.130 Electron/7.1.5 Safari/537.36")
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((r, e) -> {
                    if (e != null) {
                        log("Exception fetching lyrics for " + song.getInternalName());
                        e.printStackTrace();
                        future.complete(null);
                        return;
                    }

                    if (r.statusCode() != 200) {
                        log("HTTP error (" + r.statusCode() + ") fetching lyrics for " + song.getInternalName() + ":\n" + r.body());
                        future.complete(null);
                        return;
                    }

                    var json = new Gson().fromJson(r.body(), JsonObject.class);

                    var parent = json
                            .getAsJsonObject("message")
                            .getAsJsonObject("body")
                            .getAsJsonObject("macro_calls")
                            .getAsJsonObject("track.subtitles.get")
                            .getAsJsonObject("message")
                            .getAsJsonObject("body");

                    var subtitleList = parent.getAsJsonArray("subtitle_list");

                    if (subtitleList.size() == 0) {
                        log("Subtitle list is empty!");
                        future.complete(null);
                        return;
                    }

                    var subtitles = subtitleList.get(0)
                            .getAsJsonObject()
                            .getAsJsonObject("subtitle")
                            .get("subtitle_body").getAsString();

                    if (subtitles == null) {
                        log("Subtitles are null!");
                        future.complete(null);
                        return;
                    }

                    try {
                        log("Inserting...");
                        lyrics.insertOne(new Document("_id", song.getInternalName()).append("subtitles", subtitles).append("rawJson", r.body()));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        future.complete(null);
                        return;
                    }

                    log("Fetched lyrics successfully for " + song.getInternalName());

                    future.complete(new LiveLyrics(subtitles));
                });

        return future;
    }
}