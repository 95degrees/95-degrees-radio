package me.voidinvoid.discordmusic.songs;

import me.voidinvoid.discordmusic.utils.AlbumArt;

import java.nio.file.Path;
import java.util.function.Function;

public enum SongType {

    SONG("Song", true, true, s -> null),
    JINGLE("Jingle", false, false, s -> AlbumArt.JINGLE_ALBUM_ART),
    SPECIAL("Special", false, false, s -> AlbumArt.JINGLE_ALBUM_ART), //todo
    ADVERTISEMENT("Advert", false, false, s -> AlbumArt.ADVERT_ALBUM_ART),
    QUIZ("Quiz Question", false, false, s -> AlbumArt.ADVERT_ALBUM_ART); //todo

    private final String displayName;
    private final boolean announce;
    private final boolean useStatus;
    private final Function<Song, Path> getAlbumArt;

    SongType(String displayName, boolean announce, boolean useStatus, Function<Song, Path> getAlbumArt) {

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

    public Path getAlbumArt(Song song) {
        return getAlbumArt.apply(song);
    }
}
