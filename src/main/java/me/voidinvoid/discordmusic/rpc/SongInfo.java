package me.voidinvoid.discordmusic.rpc;

import me.voidinvoid.discordmusic.songs.*;
import me.voidinvoid.discordmusic.songs.albumart.RemoteAlbumArt;
import me.voidinvoid.discordmusic.songs.database.DatabaseSong;
import net.dv8tion.jda.api.entities.User;

public class SongInfo {

    public String name;
    public String artist;
    public String albumArtUrl;
    public long startTime;
    public long duration;
    public boolean canBeRated = false;

    public UserInfo suggestedBy;

    public SongInfo(String name, String artist, String albumArtUrl, long startTime, long duration, boolean canBeRated, User suggestedBy) {

        this.name = name;
        this.artist = artist;
        this.albumArtUrl = albumArtUrl;
        this.startTime = startTime;
        this.duration = duration;

        this.canBeRated = canBeRated;

        if (suggestedBy != null) this.suggestedBy = new UserInfo(suggestedBy);
    }

    public SongInfo(DatabaseSong song, String albumArtUrl, long startTime, long duration, User suggestedBy) {

        this.name = song.getTitle();
        this.artist = song.getArtist();
        this.albumArtUrl = albumArtUrl;
        this.startTime = startTime;
        this.duration = duration;

        this.canBeRated = true;

        if (suggestedBy != null) this.suggestedBy = new UserInfo(suggestedBy);
    }

    public SongInfo(Song song) {

        this.startTime = -1;
        this.duration = 0;

        if (song.getType() != SongType.SONG) {
            this.name = "95 Degrees Radio";
            this.artist = "";

        } else {
            this.name = song.getTitle();
            this.artist = song.getArtist();
        }

        if (song instanceof UserSuggestable) {
            var s = (UserSuggestable) song;
            if (s.getSuggestedBy() != null) {
                this.suggestedBy = new UserInfo(s.getSuggestedBy());
            }
        }

        this.canBeRated = song instanceof DatabaseSong || song instanceof SpotifySong;

        var albumArt = song.getAlbumArt();

        this.albumArtUrl = albumArt instanceof RemoteAlbumArt ? ((RemoteAlbumArt) albumArt).getUrl() : null;
    }
}
