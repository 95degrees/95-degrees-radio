package me.voidinvoid.discordmusic.events;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.utils.cache.CachedChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.EventListener;

import javax.annotation.Nonnull;

public class SkipManager implements RadioService, SongEventListener, EventListener {

    private int currentSkipRequests;
    private int skipThreshold;

    private CachedChannel<VoiceChannel> radioChannel;

    @Override
    public void onLoad() {
        radioChannel = new CachedChannel<>(RadioConfig.config.channels.voice);
    }

    private void calculateSkipThreshold() {
        skipThreshold = (int) ((radioChannel.get().getMembers().size() - 1) * 0.75);

        log("Current song skip threshold: " + skipThreshold);

        if (currentSkipRequests >= skipThreshold) {
            Radio.getInstance().getOrchestrator().playNextSong();
        }
    }

    public int getSkipThreshold() {
        return skipThreshold;
    }

    public void addSkipRequest() {
        currentSkipRequests++;

        if (currentSkipRequests >= skipThreshold) {
            Radio.getInstance().getOrchestrator().playNextSong();
        }
    }

    public void removeSkipRequest() {
        currentSkipRequests--;
    }

    @Override
    public void onEvent(@Nonnull GenericEvent ev) {
        if (ev instanceof GuildVoiceUpdateEvent) {
            calculateSkipThreshold();
        }
    }
}
