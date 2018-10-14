package me.voidinvoid.tasks.types;

import me.voidinvoid.SongOrchestrator;
import me.voidinvoid.songs.Playlist;
import me.voidinvoid.songs.SongPlaylist;
import me.voidinvoid.tasks.ParameterList;
import me.voidinvoid.tasks.RadioTaskExecutor;

public class ToggleQueueTask extends RadioTaskExecutor {

    private final boolean enable;

    ToggleQueueTask(boolean enable) {
        this.enable = enable;
    }

    @Override
    public void runTask(SongOrchestrator orch, ParameterList params) {
        orch.setQueueCommandEnabled(enable);
    }
}
