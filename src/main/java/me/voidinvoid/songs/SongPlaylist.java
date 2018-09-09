package me.voidinvoid.songs;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Properties;

public class SongPlaylist {

    private SongQueue songs;
    private SongQueue jingles;

    private String name;
    private String internal;

    private boolean isDefault;
    private boolean shuffleSongs;
    private boolean jinglesEnabled;

    public SongPlaylist(File dir) {
        String dirName = dir.toString();
        internal = dir.getName();

        Properties prop = new Properties();

        try {

            InputStream input = new FileInputStream(Paths.get(dir.toString(), "playlist-info.txt").toString());
            prop.load(input);

            shuffleSongs = Boolean.parseBoolean(prop.getProperty("shuffle", "true"));
            name = prop.getProperty("name", dir.getName());
            isDefault = Boolean.parseBoolean(prop.getProperty("default", "false"));
            jinglesEnabled = Boolean.parseBoolean(prop.getProperty("use-jingles", "true"));

        } catch (Exception ex) {
            name = dir.getName();
            ex.printStackTrace();
        }

        songs = new SongQueue(Paths.get(dirName, "Songs"), SongType.SONG, shuffleSongs);
        jingles = new SongQueue(Paths.get(dirName, "Jingles"), SongType.JINGLE, true);
    }

    public SongQueue getSongs() {
        return songs;
    }

    public SongQueue getJingles() {
        return jingles;
    }

    public String getName() {
        return name;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public String getInternal() {
        return internal;
    }

    public boolean isJinglesEnabled() {
        return jinglesEnabled;
    }
}
