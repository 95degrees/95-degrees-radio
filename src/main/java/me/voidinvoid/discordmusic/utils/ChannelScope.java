package me.voidinvoid.discordmusic.utils;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.config.RadioConfig;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;

import java.util.function.Function;

public enum ChannelScope {
    RADIO_CHAT(t -> t.getId().equals(RadioConfig.config.channels.radioChat), m -> m.hasPermission(Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat), Permission.VIEW_CHANNEL)),
    DJ_CHAT(t -> t.getId().equals(RadioConfig.config.channels.djChat), m -> m.hasPermission(Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.djChat), Permission.VIEW_CHANNEL)),
    RADIO_AND_DJ_CHAT(t -> t.getId().equals(RadioConfig.config.channels.radioChat) || t.getId().equals(RadioConfig.config.channels.djChat), m -> m.hasPermission(Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat), Permission.VIEW_CHANNEL)),
    RADIO_VOICE(t -> t.getId().equals(RadioConfig.config.channels.voice), m -> m.hasPermission(Radio.getInstance().getJda().getVoiceChannelById(RadioConfig.config.channels.voice), Permission.VOICE_CONNECT));

    private Function<GuildChannel, Boolean> checkChannel;
    private Function<Member, Boolean> checkMember;

    ChannelScope(Function<GuildChannel, Boolean> checkChannel, Function<Member, Boolean> checkMember) {

        this.checkChannel = checkChannel;
        this.checkMember = checkMember;
    }

    public boolean check(GuildChannel channel) {
        return checkChannel.apply(channel);
    }

    public boolean hasAccess(Member member) {
        return checkMember.apply(member);
    }
}
