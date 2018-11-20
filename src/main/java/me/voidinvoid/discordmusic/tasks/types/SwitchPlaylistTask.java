package me.voidinvoid.discordmusic.tasks.types;

import me.voidinvoid.discordmusic.SongOrchestrator;
import me.voidinvoid.discordmusic.songs.Playlist;
import me.voidinvoid.discordmusic.songs.SongPlaylist;
import me.voidinvoid.discordmusic.tasks.ParameterList;
import me.voidinvoid.discordmusic.tasks.RadioTaskExecutor;

import java.util.Optional;

public class SwitchPlaylistTask extends RadioTaskExecutor {

    @Override
    public void runTask(SongOrchestrator orch, ParameterList params) {
        String playlist = params.get("playlist_name", String.class);
        boolean force = params.get("switch_instantly", Boolean.class);

        Optional<Playlist> first = orch.getPlaylists().stream().filter(SongPlaylist.class::isInstance).filter(p -> p.getInternal().equalsIgnoreCase(playlist)).findAny();

        if (!first.isPresent()) {
            System.out.println("TASK: no playlist (internal name) found matching playlist_name: " + playlist);
            return;
        }

        orch.setActivePlaylist(first.get());

        if (force) {
            orch.getPlayer().setPaused(false);
            orch.getPlayer().stopTrack();
        }
    }
}
