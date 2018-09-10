package me.voidinvoid.tasks.types;

import me.voidinvoid.SongOrchestrator;
import me.voidinvoid.config.RadioConfig;
import me.voidinvoid.tasks.ParameterList;
import me.voidinvoid.tasks.RadioTaskExecutor;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;

import java.awt.*;
import java.time.OffsetDateTime;

public class KaraokeTask extends RadioTaskExecutor {

    private boolean start;

    public KaraokeTask(boolean start) {
        this.start = start;
    }

    @Override
    public void runTask(SongOrchestrator orch, ParameterList params) {
        orch.getRadio().getKaraokeManager().setKaraokeMode(start, orch.getJda().getTextChannelById(RadioConfig.config.channels.lyricsChat)); //todo channel param
    }
}
