package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.songs.Playlist;
import me.voidinvoid.discordmusic.songs.SongPlaylist;
import me.voidinvoid.discordmusic.utils.ChannelScope;

import java.util.stream.Collectors;

public class PlaylistsCommand extends Command {

    PlaylistsCommand() {
        super("playlists", "Lists all playlists", null, ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        Playlist active = Radio.instance.getOrchestrator().getActivePlaylist();
        data.code("[Playlists]\n\n" + Radio.instance.getOrchestrator().getPlaylists().stream().filter(SongPlaylist.class::isInstance).map(SongPlaylist.class::cast).map(p -> p.getName() + (p.isTestingMode() ? " [TEST MODE]" : "") + (active.equals(p) ? " (ACTIVE) [" : " [") + p.getInternal() + "]").collect(Collectors.joining("\n")));
    }
}
