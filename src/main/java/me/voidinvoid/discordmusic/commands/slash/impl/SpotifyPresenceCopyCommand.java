package me.voidinvoid.discordmusic.commands.slash.impl;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.commands.slash.CommandHandler;
import me.voidinvoid.discordmusic.commands.slash.SlashCommandData;
import me.voidinvoid.discordmusic.commands.slash.SlashCommandHandler;
import me.voidinvoid.discordmusic.songs.RadioPlaylist;
import me.voidinvoid.discordmusic.spotify.SpotifyManager;
import me.voidinvoid.discordmusic.suggestions.SongSuggestionManager;
import me.voidinvoid.discordmusic.suggestions.SuggestionQueueMode;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public class SpotifyPresenceCopyCommand implements SlashCommandHandler {

    @CommandHandler
    public void copyme(SlashCommandData data) {

        if (!(Radio.getInstance().getOrchestrator().getActivePlaylist() instanceof RadioPlaylist)) {
            data.error("This command can only be used when a song playlist is active");
            return;
        }

        var activities = data.getMember().getActivities();

        for (var activity : activities) {
            if (!activity.isRich()) {
                continue;
            }

            var rp = activity.asRichPresence();
            if (rp == null) {
                continue;
            }

            if (rp.getName().equals("Spotify") && rp.getType() == Activity.ActivityType.LISTENING && rp.getSyncId() != null) {
                var track = SpotifyManager.SPOTIFY_TRACK_URL + rp.getSyncId();

                Radio.getInstance().getService(SongSuggestionManager.class)
                        .addSuggestion(track, data.getEvent().deferReply().submit(), data.getMember(), data.getEvent().getTextChannel(), true, data.getBooleanOption("autoselect", true), SuggestionQueueMode.NORMAL);
                return;
            }
        }

        data.error("Couldn't find the Spotify song you're listening to! Make sure you have Spotify linked and enabled within your Discord settings", true);
    }

    @Override
    public CommandData getCommand() {
        return new CommandData("copyme", "Queues the song which you are currently playing on your Spotify status");
    }

    @Override
    public boolean requiresDjAccess() {
        return false;
    }
}
