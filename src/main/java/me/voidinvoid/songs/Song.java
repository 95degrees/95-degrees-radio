package me.voidinvoid.songs;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public abstract class Song {

    private SongType songType;

    private SongQueue queue;

    public Song(SongType songType) {
        this.songType = songType;
    }

    public AudioTrack getTrack() {
        return null;
    }

    public abstract String getLocation();

    public abstract String getIdentifier();

    public abstract AlbumArtType getAlbumArtType();

    public abstract File getAlbumArtFile();

    public String getAlbumArtURL() {
        return null;
    }

    public abstract boolean isPersistent();

    public SongType getType() {
        return songType;
    }

    public SongQueue getQueue() {
        return queue;
    }

    public void setQueue(SongQueue queue) {
        this.queue = queue;
    }
}