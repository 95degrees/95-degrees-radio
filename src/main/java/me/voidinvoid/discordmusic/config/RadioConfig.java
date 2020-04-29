package me.voidinvoid.discordmusic.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import org.bson.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RadioConfig {

    public static RadioConfig config;

    public String botToken;
    public String restreamBotToken;
    public String voiceInviteLink;
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

    public static class Roles {
        //public String notificationsOptOutRole;
        public String quizInGameRole, quizEliminatedRole;
    }

    public static class Channels {
        public String voice, radioChat, djChat, leaderboards;
    }

    public static class Locations {
        @Deprecated
        public String playlists;
        public String specialPlaylist;
        @Deprecated
        public String advertPlaylist;
        public String quizzes;
        public String tasks;
        @Deprecated
        public String adverts;
        @Deprecated
        public String recordings;
        public String songCache;
        public String[] rewardIdentifiers;
    }

    public static class Images {
        public String fallbackAlbumArt;
        public String jingleAlbumArt;
        public String advertAlbumArt;
        public String networkAlbumArt;
        public String specialAlbumArt;
        public String levellingUpLogo;
        public String achievementLogo;
    }

    public static class Orchestration {
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
