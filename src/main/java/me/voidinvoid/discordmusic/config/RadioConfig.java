package me.voidinvoid.discordmusic.config;

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
        public String fallbackAlbumArt, jingleAlbumArt, advertAlbumArt, networkAlbumArt;
    }

    public class Orchestration {
        public int jingleFrequency;
        public long maxSongLength;
        public int userQueueLimit;
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
