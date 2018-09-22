package me.voidinvoid.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.nio.file.Files;

public class RadioConfig {

    public static RadioConfig config;

    public String botToken;
    public boolean useCoinGain;
    public boolean debug;
    public boolean useStatus;
    public boolean useAdverts;
    @Deprecated
    public boolean liveFileUpdates;

    public Channels channels;
    public Locations locations;
    public Images images;

    public String notificationsOptOutRole;

    public class Channels {
        public String voice, radioChat, djChat, lyricsChat;
    }

    public class Locations {
        public String coinUpdates, playlists, specialPlaylist, advertPlaylist, tasks, adverts, recordings;
    }

    public class Images {
        public String fallbackAlbumArt, jingleAlbumArt, advertAlbumArt, networkAlbumArt;
    }

    public static boolean loadFromFile(File file) {
        try {
            config = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().fromJson(new String(Files.readAllBytes(file.toPath())), RadioConfig.class);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
