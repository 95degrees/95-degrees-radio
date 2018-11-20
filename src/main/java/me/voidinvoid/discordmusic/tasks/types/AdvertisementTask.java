package me.voidinvoid.discordmusic.tasks.types;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.SongOrchestrator;
import me.voidinvoid.discordmusic.tasks.ParameterList;
import me.voidinvoid.discordmusic.tasks.RadioTaskExecutor;

public class AdvertisementTask extends RadioTaskExecutor {

    @Override
    public void runTask(SongOrchestrator orch, ParameterList params) {
        if (Radio.instance.getAdvertisementManager() == null) return;

        Radio.instance.getAdvertisementManager().pushAdvertisement();
    }
}
