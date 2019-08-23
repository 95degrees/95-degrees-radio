package me.voidinvoid.discordmusic.utils;

import me.voidinvoid.discordmusic.config.RadioConfig;
import net.dv8tion.jda.api.entities.GuildChannel;

import java.util.function.Function;

public enum ChannelScope {
    RADIO_CHAT(t -> t.getId().equals(RadioConfig.config.channels.radioChat)),
    DJ_CHAT(t -> t.getId().equals(RadioConfig.config.channels.djChat)),
    RADIO_AND_DJ_CHAT(t -> t.getId().equals(RadioConfig.config.channels.radioChat) || t.getId().equals(RadioConfig.config.channels.djChat)),
    RADIO_VOICE(t -> t.getId().equals(RadioConfig.config.channels.voice));

    private Function<GuildChannel, Boolean> check;

    ChannelScope(Function<GuildChannel, Boolean> check) {

        this.check = check;
    }

    public boolean check(GuildChannel channel) {
        return check.apply(channel);
    }
}
