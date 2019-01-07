package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.songs.Playlist;
import me.voidinvoid.discordmusic.songs.RadioPlaylist;
import me.voidinvoid.discordmusic.utils.ChannelScope;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SwitchPlaylistCommand extends Command {

    SwitchPlaylistCommand() {
        super("switch-playlist", "Switches to a playlist (based on folder name)", "<name>", ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        String[] args = data.getArgs();

        List<Playlist> playlists = Radio.getInstance().getOrchestrator().getPlaylists().stream().filter(RadioPlaylist.class::isInstance).collect(Collectors.toList());

        if (args.length < 1) {
            data.error("Playlist name required. Valid playlists: " + String.join(", ", playlists.stream().map(Playlist::getInternal).collect(Collectors.toList())));
            return;
        }

        Optional<Playlist> playlist = playlists.stream().filter(p -> p.getInternal().equalsIgnoreCase(data.getArgsString())).findAny();

        if (!playlist.isPresent()) {
            data.error("Couldn't find that playlist");
            return;
        }

        Radio.getInstance().getOrchestrator().setActivePlaylist(playlist.get());
    }
}
