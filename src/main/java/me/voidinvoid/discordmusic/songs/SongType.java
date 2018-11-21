package me.voidinvoid.discordmusic.songs;

import java.nio.file.Path;
import java.util.function.Function;

public enum SongType {

    SONG("Song", true, true, s -> null),
    JINGLE("Jingle", false, false, s -> FileSong.JINGLE_ALBUM_ART),
    SPECIAL("Special", false, false, s -> FileSong.JINGLE_ALBUM_ART), //todo
    ADVERTISEMENT("Advert", false, false, s -> FileSong.ADVERT_ALBUM_ART),
    QUIZ("Quiz Question", false, false, s -> FileSong.ADVERT_ALBUM_ART);

    private final String displayName;
    private final boolean announce;
    private final boolean useStatus;
    private final Function<FileSong, Path> getAlbumArt;

    SongType(String displayName, boolean announce, boolean useStatus, Function<FileSong, Path> getAlbumArt) {

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

    public Path getAlbumArt(FileSong song) {
        return getAlbumArt.apply(song);
    }
}
