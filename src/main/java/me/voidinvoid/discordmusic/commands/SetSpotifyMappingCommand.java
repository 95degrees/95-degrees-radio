package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.lyrics.LiveLyricsManager;
import me.voidinvoid.discordmusic.spotify.SpotifyManager;
import me.voidinvoid.discordmusic.utils.Formatting;
import me.voidinvoid.discordmusic.utils.Rank;
import me.voidinvoid.discordmusic.utils.Service;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2020
 */
public class SetSpotifyMappingCommand extends Command {

    SetSpotifyMappingCommand() {
        super("map", "Sets the YouTube song mapping for the specified Spotify track ID", "<spotify id> <youtube url>", Rank.DJ);
    }

    @Override
    public void invoke(CommandData data) {
        if (data.getArgs().length < 1) {
            data.error("Spotify track ID required");
            return;
        }

        if (data.getArgs().length < 2) {
            data.error("YouTube (or other identifier) URL required");
            return;
        }

        var spotify = data.getArgs()[0];
        var identifier = data.getArgs()[1];

        var sm = Service.of(SpotifyManager.class);
        var llm = Service.of(LiveLyricsManager.class);

        sm.getSpotifyApi().getTrack(spotify).build().executeAsync().whenComplete((t, ex) -> {
            if (ex != null || t == null) {
                data.error("Error finding that track on Spotify. Spotify track IDs are case sensitive");
                return;
            }

            llm.removeLyrics(t.getId());

            sm.saveCachedIdentifier(t, identifier);
            data.success("Saved mapping for **" + Formatting.escape(t.getName()) + "**. Playlists must be reloaded for this to take effect");
        });
    }
}
