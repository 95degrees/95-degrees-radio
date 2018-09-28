package me.voidinvoid.commands;

import me.voidinvoid.Radio;
import me.voidinvoid.songs.SongPlaylist;
import me.voidinvoid.utils.ChannelScope;

import java.util.stream.Collectors;

public class PlaylistsCommand extends Command {

    PlaylistsCommand() {
        super("playlists", "Lists all playlists", null, ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        SongPlaylist active = Radio.instance.getOrchestrator().getActivePlaylist();
        data.getTextChannel().sendMessage("```[Playlists]\n\n" + Radio.instance.getOrchestrator().getPlaylists().stream().map(p -> p.getName() + (p.isTestingMode() ? " [TEST MODE]" : "") + (active.equals(p) ? " (ACTIVE) [" : " [") + p.getInternal() + "]").collect(Collectors.joining("\n")) + "```").queue();
    }
}
