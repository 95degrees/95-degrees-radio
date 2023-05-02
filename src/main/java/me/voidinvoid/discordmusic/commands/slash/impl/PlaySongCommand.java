package me.voidinvoid.discordmusic.commands.slash.impl;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.commands.slash.CommandHandler;
import me.voidinvoid.discordmusic.commands.slash.SlashCommandData;
import me.voidinvoid.discordmusic.commands.slash.SlashCommandHandler;
import me.voidinvoid.discordmusic.songs.RadioPlaylist;
import me.voidinvoid.discordmusic.suggestions.SongSuggestionManager;
import me.voidinvoid.discordmusic.suggestions.SuggestionQueueMode;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class PlaySongCommand implements SlashCommandHandler {

    @CommandHandler
    public void play(SlashCommandData data) {

        if (!(Radio.getInstance().getOrchestrator().getActivePlaylist() instanceof RadioPlaylist)) {
            data.error("This command can only be used when a song playlist is active");
            return;
        }

        var track = data.getStringOption("track", null);

        var hookFuture = data.getEvent().deferReply().submit();

        Radio.getInstance().getService(SongSuggestionManager.class)
                .addSuggestion((track.startsWith("https://") || track.startsWith("http://") ? track : "ytsearch:" + track), hookFuture, data.getMember(), data.getEvent().getTextChannel(), true, data.getBooleanOption("autoselect", true), SuggestionQueueMode.NORMAL);
    }

    @Override
    public CommandData getCommand() {
        return new CommandData("play", "Plays the specified track")
                .addOptions(new OptionData(OptionType.STRING, "track", "The name or URL of the track").setRequired(true))
                .addOptions(new OptionData(OptionType.BOOLEAN, "autoselect", "Automatically select the first track if there are multiple matches").setRequired(false));
    }

    @Override
    public boolean requiresDjAccess() {
        return false;
    }
}
