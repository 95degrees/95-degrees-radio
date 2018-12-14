package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.songs.RadioPlaylist;
import me.voidinvoid.discordmusic.suggestions.SuggestionQueueMode;
import me.voidinvoid.discordmusic.utils.ChannelScope;

public class YouTubeSearchCommand extends Command {

    YouTubeSearchCommand() {
        super("play", "Searches for a specified song on YouTube", "<search ...>", ChannelScope.RADIO_AND_DJ_CHAT, "search");
    }

    @Override
    public void invoke(CommandData data) {
        if (data.isConsole()) {
            data.error("This command can't be ran by console"); //todo move into command parameter?
            return;
        }

        if (!(Radio.instance.getOrchestrator().getActivePlaylist() instanceof RadioPlaylist)) {
            data.error("This command can only be used when a song playlist is active");
            return;
        }

        String[] args = data.getArgs();

        if (args.length < 1) {
            data.error("YouTube search required");
            return;
        }

        Radio.instance.getSuggestionManager().addSuggestion("ytsearch:" + data.getArgsString(), data.getRawMessage(), data.getTextChannel(), data.getMember(), true, SuggestionQueueMode.NORMAL);
    }
}
