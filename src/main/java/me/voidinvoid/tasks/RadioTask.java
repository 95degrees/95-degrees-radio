package me.voidinvoid.tasks;

import me.voidinvoid.SongOrchestrator;

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
