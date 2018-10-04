package me.voidinvoid.songs;

public abstract class Playlist {
    protected SongQueue songs;
    protected SongQueue jingles;
    protected String name;
    protected String internal;
    protected boolean isDefault;
    protected double coinMultiplier;
    protected String statusOverrideMessage;

    public Playlist(String internal) {
        this.internal = internal;
    }

    public SongQueue getSongs() {
        return songs;
    }

    public SongQueue getJingles() {
        return jingles;
    }

    public String getName() {
        return name;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public String getInternal() {
        return internal;
    }

    public String getStatusOverrideMessage() {
        return statusOverrideMessage;
    }

    public double getCoinMultiplier() {
        return coinMultiplier;
    }

    public abstract void awaitLoad();

    public abstract Song provideNextSong(boolean playJingle); //TODO
}
