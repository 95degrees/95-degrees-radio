package me.voidinvoid.discordmusic.songs;

import com.wrapper.spotify.model_objects.specification.Track;
import me.voidinvoid.discordmusic.songs.albumart.AlbumArt;
import me.voidinvoid.discordmusic.songs.albumart.RemoteAlbumArt;
import me.voidinvoid.discordmusic.spotify.SpotifyManager;
import me.voidinvoid.discordmusic.utils.Service;

public class SpotifySong extends Song implements SpotifyTrackHolder {

    private Track spotifyTrack;
    private String identifier;
    private AlbumArt albumArt;

    public SpotifySong(SongType songType, Track spotifyTrack) {
        super(songType);

        this.spotifyTrack = spotifyTrack;
        this.identifier = Service.of(SpotifyManager.class).getIdentifier(spotifyTrack);

        this.albumArt = spotifyTrack.getAlbum() == null || spotifyTrack.getAlbum().getImages() == null ?
                null : new RemoteAlbumArt(spotifyTrack.getAlbum().getImages()[0].getUrl());
    }

    @Override
    public Track getSpotifyTrack() {
        return spotifyTrack;
    }

    @Override
    public void setSpotifyTrack(Track track) {
        //do nothing, don't want our track touched >:D
    }

    @Override
    public String getInternalName() {
        return spotifyTrack.getId();
    }

    @Override
    public String getLavaIdentifier() {
        return identifier;
    }

    @Override
    public AlbumArt getAlbumArt() {
        var p = getType().getAlbumArt(this);

        if (p != null) {
            return p;
        }

        return albumArt;
    }

    @Override
    public String getTitle() {
        return spotifyTrack.getName();
    }

    @Override
    public String getArtist() {
        return spotifyTrack.getArtists()[0].getName();
    }

    @Override
    public boolean isPersistent() {
        return false;
    }
}
