package me.voidinvoid.discordmusic.songs;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.songs.albumart.AlbumArt;
import me.voidinvoid.discordmusic.songs.albumart.AlbumArtManager;

import java.util.function.BiFunction;

public enum SongType {

    SONG("Song", true, true, (s, a) -> null),
    JINGLE("Jingle", false, false, (s, a) -> a.getJingleAlbumArt()),
    SPECIAL("Special", false, false, (s, a) -> a.getSpecialAlbumArt()),
    ADVERTISEMENT("Advert", false, false, (s, a) -> a.getAdvertAlbumArt()),
    REWARD("Reward", false, false, (s, a) -> a.getSpecialAlbumArt()),
    QUIZ("Quiz Question", false, false, (s, a) -> a.getSpecialAlbumArt());

    private final String displayName;
    private final boolean announce;
    private final boolean useStatus;
    private final BiFunction<Song, AlbumArtManager, AlbumArt> getAlbumArt;

    SongType(String displayName, boolean announce, boolean useStatus, BiFunction<Song, AlbumArtManager, AlbumArt> getAlbumArt) {

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

    public AlbumArt getAlbumArt(Song song) {
        return getAlbumArt.apply(song, Radio.getInstance().getService(AlbumArtManager.class));
    }
}
