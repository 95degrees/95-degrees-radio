package me.voidinvoid.discordmusic.songs;

import com.wrapper.spotify.model_objects.specification.Track;

public interface SpotifyTrackHolder {

    Track getSpotifyTrack();

    void setSpotifyTrack(Track track);
}
