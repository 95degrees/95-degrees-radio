package me.voidinvoid.discordmusic.songs;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.songs.albumart.AlbumArt;

public abstract class Song {

    private SongType songType;

    private SongQueue queue;

    public Song(SongType songType) {
        this.songType = songType;
    }

    public AudioTrack getTrack() {
        return null;
    }

    public abstract String getInternalName();

    public abstract String getLavaIdentifier();

    public abstract AlbumArt getAlbumArt();

    public abstract String getTitle();

    public abstract String getArtist();

    public abstract boolean isPersistent();

    public SongType getType() {
        return songType;
    }

    public SongQueue getQueue() {
        return queue;
    }

    public Song setQueue(SongQueue queue) {
        this.queue = queue;
        return this;
    }
}