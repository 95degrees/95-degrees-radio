package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.songs.Playlist;
import me.voidinvoid.discordmusic.songs.RadioPlaylist;
import me.voidinvoid.discordmusic.utils.ChannelScope;

import java.util.stream.Collectors;

public class PlaylistsCommand extends Command {

    PlaylistsCommand() {
        super("playlists", "Lists all playlists", null, ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        Playlist active = Radio.getInstance().getOrchestrator().getActivePlaylist();
        data.code("[Playlists]\n\n" + Radio.getInstance().getOrchestrator().getPlaylists().stream().filter(RadioPlaylist.class::isInstance).map(RadioPlaylist.class::cast).map(p -> p.getName() + (p.isTestingMode() ? " [TEST MODE]" : "") + (active.equals(p) ? " (ACTIVE) [" : " [") + p.getInternal() + "]").collect(Collectors.joining("\n")));
    }
}
