package me.voidinvoid.discordmusic.tasks.types;

import me.voidinvoid.discordmusic.SongOrchestrator;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.karaoke.KaraokeManager;
import me.voidinvoid.discordmusic.tasks.ParameterList;
import me.voidinvoid.discordmusic.tasks.RadioTaskExecutor;

public class KaraokeTask extends RadioTaskExecutor {

    private boolean start;

    KaraokeTask(boolean start) {
        this.start = start;
    }

    @Override
    public void runTask(SongOrchestrator orch, ParameterList params) {
        orch.getRadio().getService(KaraokeManager.class).setKaraokeMode(start, orch.getJda().getTextChannelById(RadioConfig.config.channels.lyricsChat)); //todo channel param
    }
}
