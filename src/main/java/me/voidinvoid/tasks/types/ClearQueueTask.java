package me.voidinvoid.tasks.types;

import me.voidinvoid.SongOrchestrator;
import me.voidinvoid.songs.Playlist;
import me.voidinvoid.songs.SongPlaylist;
import me.voidinvoid.tasks.ParameterList;
import me.voidinvoid.tasks.RadioTaskExecutor;

public class ClearQueueTask extends RadioTaskExecutor {

    @Override
    public void runTask(SongOrchestrator orch, ParameterList params) {
        Playlist playlist = orch.getActivePlaylist();
        if (!(playlist instanceof SongPlaylist)) return;
        ((SongPlaylist) playlist).getSongs().clearNetworkSongs();
    }
}
