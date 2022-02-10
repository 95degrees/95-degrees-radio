package me.voidinvoid.discordmusic.commands.slash.impl;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.commands.slash.CommandHandler;
import me.voidinvoid.discordmusic.commands.slash.SlashCommandData;
import me.voidinvoid.discordmusic.commands.slash.SlashCommandHandler;
import me.voidinvoid.discordmusic.songs.Playlist;
import me.voidinvoid.discordmusic.songs.RadioPlaylist;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction;

public class QueueCommand implements SlashCommandHandler {

    @CommandHandler
    public void queue(SlashCommandData data) {
        Playlist active = Radio.getInstance().getOrchestrator().getActivePlaylist();

        if (!(active instanceof RadioPlaylist)) {
            data.error("This command can only be used when a song playlist is active");
            return;
        }

        if (data.getTextChannel() != null && !Radio.getInstance().getOrchestrator().isQueueCommandEnabled() && !ChannelScope.DJ_CHAT.check(data.getTextChannel())) {
            data.error("The queue command is currently disabled");
            return;
        }

        data.embed(((RadioPlaylist) active).getSongs().getFormattedQueue());
    }

    @Override
    public CommandData getCommand() {
        return new CommandData("queue", "Shows the next 10 tracks in the queue");
    }

    @Override
    public boolean requiresDjAccess() {
        return false;
    }
}
