package me.voidinvoid.songs;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.SongQueue;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public abstract class Song {

    private boolean isJingle;

    private SongQueue queue;

    public Song(boolean isJingle) {
        this.isJingle = isJingle;
    }

    public BufferedImage scaleAlbumArt(BufferedImage img) {
        BufferedImage output = new BufferedImage(128, 128, img.getType());

        Graphics2D g2d = output.createGraphics();
        g2d.drawImage(img, 0, 0, 128, 128, null);
        g2d.dispose();

        return output;
    }

    public AudioTrack getTrack() {
        return null;
    }

    public abstract String getLocation();

    public abstract String getIdentifier();

    public abstract AlbumArtType getAlbumArtType();

    public File getAlbumArtFile() { return null; }

    public String getAlbumArtURL() { return null; }

    public abstract boolean isPersistent();

    public boolean isJingle() {
        return isJingle;
    }

    public SongQueue getQueue() {
        return queue;
    }

    public void setQueue(SongQueue queue) {
        this.queue = queue;
    }
}