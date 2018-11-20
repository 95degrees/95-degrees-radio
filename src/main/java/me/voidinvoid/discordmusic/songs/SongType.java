package me.voidinvoid.discordmusic.songs;

import java.io.File;
import java.util.function.Function;

public enum SongType {

    SONG("Song", true, true, s -> null),
    JINGLE("Jingle", false, false, s -> FileSong.JINGLE_FILE),
    SPECIAL("Special", false, false, s -> FileSong.JINGLE_FILE), //todo
    ADVERTISEMENT("Advert", false, false, s -> FileSong.ADVERT_FILE),
    QUIZ("Quiz Question", false, false, s -> FileSong.ADVERT_FILE);

    private final String displayName;
    private final boolean announce;
    private final boolean useStatus;
    private final Function<FileSong, File> getAlbumArt;

    SongType(String displayName, boolean announce, boolean useStatus, Function<FileSong, File> getAlbumArt) {

        this.displayName = displayName;
        this.announce = announce;
        this.useStatus = useStatus;
        this.getAlbumArt = getAlbumArt;
    }

    public boolean useAnnouncement() {
        return announce;
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
