package me.voidinvoid.discordmusic.songs;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class RadioPlaylist extends Playlist {

    private CompletableFuture<List<Song>> songQueueFuture, jingleQueueFuture;
    private SongQueue songQueue, jingleQueue;

    public RadioPlaylist(String internal) {
        super(internal);
    }

    protected void setSongQueue(SongQueue queue) {
        songQueue = queue;
        songQueueFuture = queue.loadSongsAsync();
    }

    protected void setJingleQueue(SongQueue queue) {
        jingleQueue = queue;
        jingleQueueFuture = queue.loadSongsAsync();
    }

    @Override
    public abstract RadioPlaylistProperties getProperties();

    public SongQueue getSongs() {
        return songQueue;
    }

    public SongQueue getJingles() {
        return jingleQueue;
    }

    public boolean isJinglesEnabled() {
        return getProperties().isJinglesEnabled();
    }

    public boolean isTestingMode() {
        return getProperties().isTestingMode();
    }

    public boolean isDirectMessageNotifications() {
        return getProperties().isDirectMessageNotifications();
    }

    @Override
    public void awaitLoad() {
        if (songQueueFuture.isDone() && jingleQueueFuture.isDone()) return; //already loaded

        try {
            songQueueFuture.get();
            jingleQueueFuture.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Song provideNextSong(boolean playJingle) {
        if (playJingle && getProperties().isJinglesEnabled()) return jingleQueue.getRandom();

        return songQueue.getNextAndMoveToEnd();
    }
}
