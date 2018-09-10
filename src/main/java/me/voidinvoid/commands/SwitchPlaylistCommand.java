package me.voidinvoid.commands;

import me.voidinvoid.DiscordRadio;
import me.voidinvoid.songs.Song;
import me.voidinvoid.songs.SongPlaylist;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SwitchPlaylistCommand extends Command {

    public SwitchPlaylistCommand() {
        super("switch-playlist", "Switches to a playlist (based on folder name)", "<name>", CommandScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        String[] args = data.getArgs();

        List<SongPlaylist> playlists = DiscordRadio.instance.getOrchestrator().getPlaylists();

        if (args.length < 1) {
            data.error("Playlist name required. Valid playlists: " + String.join(", ", playlists.stream().map(SongPlaylist::getInternal).collect(Collectors.toList())));
            return;
        }

        Optional<SongPlaylist> playlist = playlists.stream().filter(p -> p.getInternal().equalsIgnoreCase(args[0])).findFirst();

        if (!playlist.isPresent()) {
            data.error("Couldn't find that playlist");
            return;
        }

        DiscordRadio.instance.getOrchestrator().setActivePlaylist(playlist.get());
    }
}
