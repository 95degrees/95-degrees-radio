package me.voidinvoid.discordmusic.songs;

public class PlaylistProperties {

    private String displayName;
    private boolean isDefault;
    private boolean shuffleSongs;
    private String statusOverrideMessage;
    private double coinMultiplier;

    public String getDisplayName() {
        return displayName;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isShuffleSongs() {
        return shuffleSongs;
    }

    public String getStatusOverrideMessage() {
        return statusOverrideMessage;
    }

    public double getCoinMultiplier() {
        return coinMultiplier;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public void setShuffleSongs(boolean shuffleSongs) {
        this.shuffleSongs = shuffleSongs;
    }

    public void setStatusOverrideMessage(String statusOverrideMessage) {
        this.statusOverrideMessage = statusOverrideMessage;
    }

    public void setCoinMultiplier(double coinMultiplier) {
        this.coinMultiplier = coinMultiplier;
    }
}
