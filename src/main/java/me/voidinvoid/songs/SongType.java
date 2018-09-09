package me.voidinvoid.songs;

public enum SongType {

    SONG("Song", true),
    JINGLE("Jingle", false),
    SPECIAL("Special", false);

    private final String displayName;
    private final boolean useStatus;

    SongType(String displayName, boolean useStatus) {

        this.displayName = displayName;
        this.useStatus = useStatus;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean usesStatus() {
        return useStatus;
    }
}
