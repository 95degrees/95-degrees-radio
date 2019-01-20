package me.voidinvoid.discordmusic.levelling;

import me.voidinvoid.discordmusic.utils.ChannelScope;
import net.dv8tion.jda.core.entities.GuildVoiceState;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.hooks.EventListener;

public class LevellingManager implements EventListener {

    @Override
    public void onEvent(Event ev) {
        if (ev instanceof GuildVoiceJoinEvent || ev instanceof GuildVoiceMoveEvent) {
            GuildVoiceState vs = ((GenericGuildVoiceEvent) ev).getVoiceState();

            if (vs.inVoiceChannel() && ChannelScope.RADIO_VOICE.check(vs.getChannel())) { //joined vc

            }
        }
    }

    public void rewardExperience(User user, int amount) {
        
    }
}
