package me.voidinvoid.discordmusic.tasks.types;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.SongOrchestrator;
import me.voidinvoid.discordmusic.tasks.ParameterList;
import me.voidinvoid.discordmusic.tasks.RadioTaskExecutor;

public class ShutdownTask extends RadioTaskExecutor {

    @Override
    public void runTask(SongOrchestrator orch, ParameterList params) {
        Radio.getInstance().shutdown(false);
    }
}