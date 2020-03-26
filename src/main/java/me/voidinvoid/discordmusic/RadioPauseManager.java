package me.voidinvoid.discordmusic;

import me.voidinvoid.discordmusic.config.RadioConfig;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.EventListener;

import javax.annotation.Nonnull;

public class RadioPauseManager implements RadioService, EventListener {

    private int prevCount;

    @Override
    public void onEvent(@Nonnull GenericEvent ev) {
        if (ev instanceof GuildVoiceUpdateEvent) {
            var e = (GuildVoiceUpdateEvent) ev;

            if (e.getChannelLeft() != null && e.getChannelLeft().getId().equals(RadioConfig.config.channels.voice)) { //LEFT
                updateChannel();

            } else if (e.getChannelJoined() != null && e.getChannelJoined().getId().equals(RadioConfig.config.channels.voice)) { //JOINED
                updateChannel();
            }
        }
    }

    public void updateChannel() {
        var vc = Radio.getInstance().getJda().getVoiceChannelById(RadioConfig.config.channels.voice);

        if (vc == null) {
            log("VC is null??");
            return;
        }

        var count = vc.getMembers().size() - 1;

        if (count <= 0) {
            Radio.getInstance().getOrchestrator().setPaused(true);
            log("Auto-radio pause due to member count of 0");
        } else if (prevCount <= 0) {
            Radio.getInstance().getOrchestrator().setPaused(false);
            log("Auto-radio resume");
        }

        prevCount = count;
    }
}
