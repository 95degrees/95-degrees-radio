package me.voidinvoid.discordmusic.tasks.types;

import me.voidinvoid.discordmusic.SongOrchestrator;
import me.voidinvoid.discordmusic.tasks.ParameterList;
import me.voidinvoid.discordmusic.tasks.RadioTaskExecutor;

public class PauseSongTask extends RadioTaskExecutor {

    private boolean pause;

    public PauseSongTask(boolean pause) {

        this.pause = pause;
    }

    @Override
    public void runTask(SongOrchestrator orch, ParameterList params) {
        orch.setPaused(pause);
    }
}
