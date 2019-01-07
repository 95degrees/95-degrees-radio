package me.voidinvoid.discordmusic.songs.database;

import me.voidinvoid.discordmusic.songs.AlbumArtType;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.SongType;
import me.voidinvoid.discordmusic.utils.AlbumArt;
import org.bson.Document;

import java.nio.file.Path;
import java.nio.file.Paths;

public class DatabaseSong extends Song {

    private String title;
    private String artist;
    private String mbId;
    private String source;
    private String albumArtId;
    private Path albumArt;

    public DatabaseSong(SongType songType, Document document) {
        super(songType);

        title = document.getString("title");
        artist = document.getString("artist");
        mbId = document.getString("mbId");
        source = document.getString("source");
        albumArtId = document.getString("albumArt");

        if (albumArtId != null) {
            albumArt = Paths.get("/projects/radio/AlbumArt/Songs/" + albumArtId + ".png");
        }
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getMbId() {
        return mbId;
    }

    @Override
    public String getFileName() {
        return "DB/" + albumArtId; //todo?
    }

    @Override
    public String getFullLocation() {
        return source;
    }

    @Override
    public AlbumArtType getAlbumArtType() {
        return AlbumArtType.FILE;
    }

    @Override
    public Path getAlbumArtFile() {
        Path p = getType().getAlbumArt(this);

        //does the song type have a specific album art? if so return that
        //otherwise does it have its own album art? if so return that
        //otherwise return the 'not found' fallback album art
        return p == null ? albumArt == null ? AlbumArt.FALLBACK_ALBUM_ART : albumArt : p;
    }

    @Override
    public boolean isPersistent() {
        return true;
    }
}
