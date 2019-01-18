package me.voidinvoid.discordmusic.continuity;

import me.voidinvoid.discordmusic.songs.NetworkSong;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
public class ContinuitySuggestionData {

    private String user;
    private String identifier;

    public ContinuitySuggestionData(NetworkSong song) {
        user = song.getSuggestedBy() == null ? null : song.getSuggestedBy().getId();
        identifier = song.getFullLocation();
    }

    public String getUser() {
        return user;
    }

    public String getIdentifier() {
        return identifier;
    }
}
