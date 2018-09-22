package me.voidinvoid.tasks.types;

import me.voidinvoid.SongOrchestrator;
import me.voidinvoid.songs.SongPlaylist;
import me.voidinvoid.tasks.ParameterList;
import me.voidinvoid.tasks.RadioTaskExecutor;

import java.util.Optional;

public class SwitchPlaylistTask extends RadioTaskExecutor {

    @Override
    public void runTask(SongOrchestrator orch, ParameterList params) {
        String playlist = params.get("playlist_name", String.class);
        boolean force = params.get("switch_instantly", Boolean.class);

        Optional<SongPlaylist> first = orch.getPlaylists().stream().filter(p -> p.getInternal().equalsIgnoreCase(playlist)).findFirst();

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
