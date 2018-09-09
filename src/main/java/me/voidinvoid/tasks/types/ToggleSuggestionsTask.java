package me.voidinvoid.tasks.types;

import me.voidinvoid.SongOrchestrator;
import me.voidinvoid.tasks.ParameterList;
import me.voidinvoid.tasks.RadioTaskExecutor;

public class ToggleSuggestionsTask extends RadioTaskExecutor {

    private boolean enable;

    public ToggleSuggestionsTask(boolean enable) {
        this.enable = enable;
    }

    @Override
    public void runTask(SongOrchestrator orch, ParameterList params) {
        orch.setSuggestionsEnabled(enable, null);
    }
}
