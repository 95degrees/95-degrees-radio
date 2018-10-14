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
        String[] args = data.getArgs();

        if (args.length > 0 && ChannelScope.DJ_CHAT.check(data.getTextChannel())) {
            if (args[0].equalsIgnoreCase("on")) {
                Radio.instance.getOrchestrator().setQueueCommandEnabled(true);
                data.success("The queue command has been enabled");
                return;

            } else if (args[0].equalsIgnoreCase("off")) {
                Radio.instance.getOrchestrator().setQueueCommandEnabled(false);
                data.success("The queue command has been disabled");
                return;
            }
        }

        Playlist active = Radio.instance.getOrchestrator().getActivePlaylist();

        if (!(active instanceof SongPlaylist)) {
            data.error("This command can only be used when a song playlist is active");
            return;
        }

        if (!Radio.instance.getOrchestrator().isQueueCommandEnabled() && !ChannelScope.DJ_CHAT.check(data.getTextChannel())) {
            data.error("The queue command is currently disabled");
            return;
        }

        data.code(((SongPlaylist) active).getSongs().getFormattedQueue());
    }
}