package me.voidinvoid.tasks;

import me.voidinvoid.SongOrchestrator;

public abstract class RadioTaskExecutor {

    public abstract void runTask(SongOrchestrator orch, ParameterList params);
}
