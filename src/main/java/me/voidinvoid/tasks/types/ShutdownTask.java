package me.voidinvoid.tasks.types;

import me.voidinvoid.DiscordRadio;
import me.voidinvoid.SongOrchestrator;
import me.voidinvoid.tasks.ParameterList;
import me.voidinvoid.tasks.RadioTaskExecutor;

public class ShutdownTask extends RadioTaskExecutor {

    @Override
    public void runTask(SongOrchestrator orch, ParameterList params) {
        orch.getJda().shutdownNow();
        //todo manager shutdown
        DiscordRadio.isRunning = false;
        //System.exit(0);
    }
}