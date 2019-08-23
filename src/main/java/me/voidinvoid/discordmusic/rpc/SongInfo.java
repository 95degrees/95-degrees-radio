package me.voidinvoid.discordmusic.rpc;

import me.voidinvoid.discordmusic.songs.NetworkSong;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.SongType;
import me.voidinvoid.discordmusic.songs.albumart.RemoteAlbumArt;
import me.voidinvoid.discordmusic.songs.database.DatabaseSong;
import net.dv8tion.jda.api.entities.User;

public class SongInfo {

    public String name;
    public String artist;
    public String albumArtUrl;
    public long startTime;
    public long endTime;
    public boolean canBeRated = false;

    public UserInfo suggestedBy;

    public SongInfo(String name, String artist, String albumArtUrl, long startTime, long endTime, boolean canBeRated, User suggestedBy) {

        this.name = name;
        this.artist = artist;
        this.albumArtUrl = albumArtUrl;
        this.startTime = startTime;
        this.endTime = endTime;
        this.canBeRated = canBeRated;

        if (suggestedBy != null) this.suggestedBy = new UserInfo(suggestedBy);
    }

    public SongInfo(DatabaseSong song, String albumArtUrl, long startTime, long endTime, User suggestedBy) {

        this.name = song.getTitle();
        this.artist = song.getArtist();
        this.albumArtUrl = albumArtUrl;
        this.startTime = startTime;
        this.endTime = endTime;

        this.canBeRated = true;

        if (suggestedBy != null) this.suggestedBy = new UserInfo(suggestedBy);
    }

    public SongInfo(Song song) {

        this.startTime = -1;
        this.endTime = -1;

        if (song.getType() != SongType.SONG) {
            this.name = "95 Degrees Radio";
            this.artist = "";

        } else if (song instanceof NetworkSong) {
            this.name = song.getTrack().getInfo().title;
            this.artist = song.getTrack().getInfo().author;

            if (((NetworkSong) song).getSuggestedBy() != null) this.suggestedBy = new UserInfo(((NetworkSong) song).getSuggestedBy());

        } else if (song instanceof DatabaseSong) {
            var ds = (DatabaseSong) song;

            this.name = ds.getTitle();
            this.artist = ds.getArtist();

            this.canBeRated = true;

        } else {
            this.name = song.getTrack().getInfo().title;
            this.artist = song.getTrack().getInfo().author;
        }

        this.albumArtUrl = song.getAlbumArt() instanceof RemoteAlbumArt ? ((RemoteAlbumArt) song.getAlbumArt()).getUrl() : null;
    }
}
