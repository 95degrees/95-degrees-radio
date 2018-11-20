package me.voidinvoid.discordmusic.tasks.types;

import me.voidinvoid.discordmusic.SongOrchestrator;
import me.voidinvoid.discordmusic.tasks.ParameterList;
import me.voidinvoid.discordmusic.tasks.RadioTaskExecutor;

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
