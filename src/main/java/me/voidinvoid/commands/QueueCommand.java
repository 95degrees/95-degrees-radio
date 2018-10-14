package me.voidinvoid.commands;

import me.voidinvoid.Radio;
import me.voidinvoid.songs.Playlist;
import me.voidinvoid.songs.SongPlaylist;
import me.voidinvoid.utils.ChannelScope;

public class QueueCommand extends Command {

    QueueCommand() {
        super("queue", "Lists the next 10 songs in the queue", null, ChannelScope.RADIO_AND_DJ_CHAT, "rq");
    }

    @Override
    public void invoke(CommandData data) {
        Playlist active = Radio.instance.getOrchestrator().getActivePlaylist();

        if (!(active instanceof SongPlaylist)) {
            data.error("This command can only be used when a song playlist is active");
            return;
        }

        data.getTextChannel().sendMessage("```" + ((SongPlaylist) active).getSongs().getFormattedQueue().replaceAll("`", "") + "```").queue();
    }
}