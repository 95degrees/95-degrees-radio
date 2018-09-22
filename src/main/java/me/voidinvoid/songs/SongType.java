package me.voidinvoid.songs;

import java.io.File;
import java.util.function.Function;

public enum SongType {

    SONG("Song", true, s -> null),
    JINGLE("Jingle", false, s -> FileSong.JINGLE_FILE),
    SPECIAL("Special", false, s -> FileSong.JINGLE_FILE), //todo
    ADVERTISEMENT("Advert", false, s -> FileSong.ADVERT_FILE);

    private final String displayName;
    private final boolean useStatus;
    private final Function<FileSong, File> getAlbumArt;

    SongType(String displayName, boolean useStatus, Function<FileSong, File> getAlbumArt) {

        this.displayName = displayName;
        this.useStatus = useStatus;
        this.getAlbumArt = getAlbumArt;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean usesStatus() {
        return useStatus;
    }

    public File getAlbumArt(FileSong song) {
        return getAlbumArt.apply(song);
    }
}
