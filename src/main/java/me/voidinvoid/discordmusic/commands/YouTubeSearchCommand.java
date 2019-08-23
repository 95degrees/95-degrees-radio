package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.songs.RadioPlaylist;
import me.voidinvoid.discordmusic.suggestions.SongSuggestionManager;
import me.voidinvoid.discordmusic.suggestions.SuggestionQueueMode;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.TextChannel;

public class YouTubeSearchCommand extends Command {

    YouTubeSearchCommand() {
        super("play", "Searches for a specified song on YouTube", "<search ...>", ChannelScope.RADIO_AND_DJ_CHAT, false, "search");
    }

    @Override
    public void invoke(CommandData data) {
        if (!(Radio.getInstance().getOrchestrator().getActivePlaylist() instanceof RadioPlaylist)) {
            data.error("This command can only be used when a song playlist is active");
            return;
        }

        String[] args = data.getArgs();

        if (args.length < 1) {
            data.error("YouTube search required");
            return;
        }

        Radio.getInstance().getService(SongSuggestionManager.class).addSuggestion("ytsearch:" + data.getArgsString(), data.getRawMessage(), (TextChannel) data.getTextChannel(), data.getMember(), true, SuggestionQueueMode.NORMAL);
    }
}
