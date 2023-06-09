package me.voidinvoid.discordmusic.tasks;

import me.voidinvoid.discordmusic.SongOrchestrator;

public class RadioTask {

    private RadioTaskExecutor type;
    private ParameterList params;

    public RadioTask(RadioTaskExecutor type, ParameterList params) {
        this.type = type;
        this.params = params;
    }

    public RadioTaskExecutor getType() {
        return type;
    }

    public ParameterList getParams() {
        return params;
    }

    public void invoke(SongOrchestrator orch) {
        type.runTask(orch, params);
    }
}
