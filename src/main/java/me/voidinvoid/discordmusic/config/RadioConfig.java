package me.voidinvoid.discordmusic.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import org.bson.Document;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RadioConfig {

    public static RadioConfig config;

    public String botToken;
    public boolean useCoinGain;
    public boolean debug;
    public boolean useStatus;
    public boolean useAdverts;
    public boolean useSocketServer;
    public boolean useQuizSocketServer;
    @Deprecated
    public boolean liveFileUpdates;

    public Channels channels;
    public Locations locations;
    public Images images;
    public Roles roles;
    public Orchestration orchestration;

    public class Roles {
        public String notificationsOptOutRole;
        public String quizInGameRole, quizEliminatedRole;
    }

    public class Channels {
        public String voice, radioChat, djChat, lyricsChat;
    }

    public class Locations {
        public String coinUpdates, playlists, specialPlaylist, advertPlaylist, quizzes, tasks, adverts, recordings;
    }

    public class Images {
        public String fallbackAlbumArt, jingleAlbumArt, advertAlbumArt, networkAlbumArt, specialAlbumArt, levellingUpLogo;
    }

    public class Orchestration {
        public int jingleFrequency;
        public long maxSongLength;
        public int userQueueLimit;
    }

    private static boolean loadFromString(String json) {
        try {
            config = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().fromJson(json, RadioConfig.class);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static boolean load(Document first) {
        if (first == null) return false;

        return loadFromString(first.toJson());
    }

    public static boolean loadFromFile(Path path) {
        try {
            return loadFromString(new String(Files.readAllBytes(path)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }
}
