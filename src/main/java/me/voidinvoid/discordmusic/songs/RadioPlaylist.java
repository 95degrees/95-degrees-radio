package me.voidinvoid.discordmusic.songs;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class RadioPlaylist extends Playlist {

    private SongQueue songs;
    private SongQueue jingles;
    private boolean shuffleSongs;
    private boolean jinglesEnabled;
    private boolean testingMode;
    private boolean directMessageNotifications;

    private CompletableFuture<List<Song>> songsFuture, jinglesFuture;

    public RadioPlaylist(Path path) {
        super(path.getFileName().toString()); //playlist dir name

        Properties prop = new Properties();

        try {

            InputStream input = new FileInputStream(path.resolve("playlist-info.txt").toString());
            prop.load(input);

            shuffleSongs = Boolean.parseBoolean(prop.getProperty("shuffle", "true"));
            name = prop.getProperty("name", path.getFileName().toString());
            isDefault = Boolean.parseBoolean(prop.getProperty("default", "false"));
            jinglesEnabled = Boolean.parseBoolean(prop.getProperty("use-jingles", "true"));
            statusOverrideMessage = prop.getProperty("discord-status", null);
            testingMode = Boolean.parseBoolean(prop.getProperty("testing", "false"));
            directMessageNotifications = Boolean.parseBoolean(prop.getProperty("direct-message-notifications", "true"));
            coinMultiplier = Double.parseDouble(prop.getProperty("coin-multiplier", "1.0"));

        } catch (Exception ex) {
            name = path.getFileName().toString();
            ex.printStackTrace();
        }

        songs = new SongQueue(this, path.resolve("Songs"), SongType.SONG, shuffleSongs);
        jingles = new SongQueue(this, path.resolve("Jingles"), SongType.JINGLE, true);

        songsFuture = songs.loadSongsAsync();
        jinglesFuture = jingles.loadSongsAsync();
    }

    public SongQueue getSongs() {
        return songs;
    }

    public SongQueue getJingles() {
        return jingles;
    }

    public boolean isJinglesEnabled() {
        return jinglesEnabled;
    }

    public boolean isTestingMode() {
        return testingMode;
    }

    public boolean isDirectMessageNotifications() {
        return directMessageNotifications;
    }

    public void awaitLoad() {
        if (songsFuture.isDone() && jinglesFuture.isDone()) return; //already loaded

        try {
            songsFuture.get();
            jinglesFuture.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Song provideNextSong(boolean playJingle) {
        if (jinglesEnabled && playJingle) return jingles.getRandom();

        return songs.getNextAndMoveToEnd();
    }
}
