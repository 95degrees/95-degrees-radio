package me.voidinvoid.discordmusic.tasks.types;

import me.voidinvoid.discordmusic.SongOrchestrator;
import me.voidinvoid.discordmusic.coins.RadioAwardsManager;
import me.voidinvoid.discordmusic.tasks.ParameterList;
import me.voidinvoid.discordmusic.tasks.RadioTaskExecutor;
import me.voidinvoid.discordmusic.utils.Service;

public class RewardTask extends RadioTaskExecutor {

    @Override
    public void runTask(SongOrchestrator orch, ParameterList params) {
        var manager = Service.of(RadioAwardsManager.class);

        if (manager == null) return;

        manager.pushAward();
    }
}
