package me.voidinvoid.discordmusic.tasks;

import me.voidinvoid.discordmusic.SongOrchestrator;

public abstract class RadioTaskExecutor {

    public abstract void runTask(SongOrchestrator orch, ParameterList params);
}
