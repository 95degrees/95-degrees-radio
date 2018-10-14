package me.voidinvoid.songs;

public abstract class Playlist {
    protected String name;
    protected String internal;
    protected boolean isDefault;
    protected double coinMultiplier;
    protected String statusOverrideMessage;

    public Playlist(String internal) {
        this.internal = internal;
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

    public void onActivate() {}

    public abstract void awaitLoad();

    public abstract Song provideNextSong(boolean playJingle);
}
