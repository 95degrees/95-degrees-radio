package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.songs.Playlist;
import me.voidinvoid.discordmusic.songs.RadioPlaylist;
import me.voidinvoid.discordmusic.utils.ChannelScope;

public class ShuffleCommand extends Command {

    ShuffleCommand() {
        super("shuffle", "Shuffles all songs in the current playlist", null, ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        Playlist active = Radio.instance.getOrchestrator().getActivePlaylist();

        if (!(active instanceof RadioPlaylist)) {
            data.error("This command can only be used when a song playlist is active");
            return;
        }

        ((RadioPlaylist) active).getSongs().shuffleQueue();
        data.success("Shuffled `" + active.getName().replaceAll("`", "") + "`");
    }
}
