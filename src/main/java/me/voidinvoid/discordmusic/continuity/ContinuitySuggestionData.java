package me.voidinvoid.discordmusic.continuity;

import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.UserSuggestable;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
public class ContinuitySuggestionData {

    private String user;
    private String identifier;

    public ContinuitySuggestionData(Song song) {
        user = song instanceof UserSuggestable && ((UserSuggestable) song).getSuggestedBy() != null ? ((UserSuggestable) song).getSuggestedBy().getId() : null;
        identifier = song.getLavaIdentifier();
    }

    public String getUser() {
        return user;
    }

    public String getIdentifier() {
        return identifier;
    }
}
