package me.voidinvoid.commands;

import me.voidinvoid.DiscordRadio;
import me.voidinvoid.songs.SongPlaylist;

import java.util.stream.Collectors;

public class ShuffleCommand extends Command {

    public ShuffleCommand() {
        super("shuffle", "Shuffles all songs in the current playlist", null, CommandScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        SongPlaylist active = DiscordRadio.instance.getOrchestrator().getActivePlaylist();
        active.getSongs().shuffleQueue();
        data.success("Shuffled `" + active.getName().replaceAll("`", "") + "`");
    }
}
