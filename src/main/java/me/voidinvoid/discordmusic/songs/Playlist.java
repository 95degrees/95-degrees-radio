package me.voidinvoid.discordmusic.songs;

public abstract class Playlist {

    protected String internal;

    protected abstract PlaylistProperties getProperties();

    public Playlist(String internal) {
        this.internal = internal;
    }

    public String getName() {
        return getProperties().getDisplayName();
    }

    public boolean isDefault() {
        return getProperties().isDefault();
    }

    public String getInternal() {
        return internal;
    }

    public String getStatusOverrideMessage() {
        return getProperties().getStatusOverrideMessage();
    }

    public double getCoinMultiplier() {
        return getProperties().getCoinMultiplier();
    }

    public void onActivate() {}

    public void onDeactivate() {}

    public abstract void awaitLoad();

    public abstract Song provideNextSong(boolean playJingle);
}
