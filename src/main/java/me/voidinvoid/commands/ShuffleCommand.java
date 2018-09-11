package me.voidinvoid.commands;

import me.voidinvoid.Radio;
import me.voidinvoid.songs.SongPlaylist;
import me.voidinvoid.utils.ChannelScope;

public class ShuffleCommand extends Command {

    public ShuffleCommand() {
        super("shuffle", "Shuffles all songs in the current playlist", null, ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        SongPlaylist active = Radio.instance.getOrchestrator().getActivePlaylist();
        active.getSongs().shuffleQueue();
        data.success("Shuffled `" + active.getName().replaceAll("`", "") + "`");
    }
}
