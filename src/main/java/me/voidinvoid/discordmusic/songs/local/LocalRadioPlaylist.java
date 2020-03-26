package me.voidinvoid.discordmusic.songs.local;

import me.voidinvoid.discordmusic.songs.RadioPlaylist;
import me.voidinvoid.discordmusic.songs.RadioPlaylistProperties;
import me.voidinvoid.discordmusic.songs.SongType;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

public class LocalRadioPlaylist extends RadioPlaylist {

    private RadioPlaylistProperties properties = new RadioPlaylistProperties();

    public LocalRadioPlaylist(Path path) {
        super(path.getFileName().toString()); //playlist dir name

        Properties prop = new Properties();

        try {

            InputStream input = new FileInputStream(path.resolve("playlist-info.txt").toString());
            prop.load(input);

            properties.setDisplayName(prop.getProperty("name", path.getFileName().toString()));
            properties.setDefault(Boolean.parseBoolean(prop.getProperty("default", "false")));
            properties.setShuffleSongs(Boolean.parseBoolean(prop.getProperty("shuffle", "true")));
            properties.setJinglesEnabled(Boolean.parseBoolean(prop.getProperty("use-jingles", "true")));
            properties.setStatusOverrideMessage(prop.getProperty("discord-status", null));
            properties.setTestingMode(Boolean.parseBoolean(prop.getProperty("testing", "false")));
            properties.setDirectMessageNotifications(Boolean.parseBoolean(prop.getProperty("direct-message-notifications", "true")));
            properties.setCoinMultiplier(Double.parseDouble(prop.getProperty("coin-multiplier", "1.0")));

        } catch (Exception ex) {
            properties.setDisplayName(path.getFileName().toString());
            ex.printStackTrace();
        }

        setSongQueue(new LocalSongQueue(path.resolve("Songs"), this, SongType.SONG, properties.isShuffleSongs()));
        setJingleQueue(new LocalSongQueue(path.resolve("Jingles"), this, SongType.JINGLE, true));
    }

    @Override
    public RadioPlaylistProperties getProperties() {
        return properties;
    }
}
