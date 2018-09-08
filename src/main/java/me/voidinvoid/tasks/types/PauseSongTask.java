package me.voidinvoid.tasks.types;

import me.voidinvoid.SongOrchestrator;
import me.voidinvoid.tasks.ParameterList;
import me.voidinvoid.tasks.RadioTaskExecutor;

public class PauseSongTask extends RadioTaskExecutor {

    private boolean pause;

    public PauseSongTask(boolean pause) {

        this.pause = pause;
    }

    @Override
    public void runTask(SongOrchestrator orch, ParameterList params) {
        orch.getPlayer().setPaused(pause);
    }
}
