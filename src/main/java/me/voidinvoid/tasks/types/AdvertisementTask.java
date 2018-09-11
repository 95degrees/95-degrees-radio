package me.voidinvoid.tasks.types;

import me.voidinvoid.Radio;
import me.voidinvoid.SongOrchestrator;
import me.voidinvoid.tasks.ParameterList;
import me.voidinvoid.tasks.RadioTaskExecutor;

public class AdvertisementTask extends RadioTaskExecutor {

    @Override
    public void runTask(SongOrchestrator orch, ParameterList params) {
        if (Radio.instance.getAdvertisementManager() == null) return;

        Radio.instance.getAdvertisementManager().pushAdvertisement();
    }
}
