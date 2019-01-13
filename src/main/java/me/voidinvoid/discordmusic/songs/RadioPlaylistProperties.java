package me.voidinvoid.discordmusic.songs;

public class RadioPlaylistProperties extends PlaylistProperties {

    private boolean jinglesEnabled;
    private boolean testingMode;
    private boolean directMessageNotifications;

    public boolean isJinglesEnabled() {
        return jinglesEnabled;
    }

    public boolean isTestingMode() {
        return testingMode;
    }

    public boolean isDirectMessageNotifications() {
        return directMessageNotifications;
    }

    public void setJinglesEnabled(boolean jinglesEnabled) {
        this.jinglesEnabled = jinglesEnabled;
    }

    public void setTestingMode(boolean testingMode) {
        this.testingMode = testingMode;
    }

    public void setDirectMessageNotifications(boolean directMessageNotifications) {
        this.directMessageNotifications = directMessageNotifications;
    }
}
