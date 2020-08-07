package me.voidinvoid.discordmusic.activity;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.restream.RadioRestreamManager;
import me.voidinvoid.discordmusic.utils.Service;
import me.voidinvoid.discordmusic.utils.cache.CachedMember;
import me.voidinvoid.discordmusic.utils.cache.ICached;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * DiscordMusic - 02/08/2020
 * This code was developed by VoidInVoid / Exfusion
 * © 2020
 */

public enum ListeningContext {

    NORMAL(g -> Utils.ACTIVE_LISTENERS.apply(g.getVoiceChannelById(RadioConfig.config.channels.voice))),
    RESTREAM(g -> {
        var vc = Service.of(RadioRestreamManager.class).getVoiceChannel();
        if (vc == null) {
            return Collections.emptyList();
        }
        return Utils.ACTIVE_LISTENERS.apply(g.getVoiceChannelById(vc));
    }),
    ALL(g -> Stream.concat(NORMAL.getListeners().stream(), RESTREAM.getListeners().stream()).distinct().collect(Collectors.toList()));

    private Function<Guild, List<CachedMember>> getListeners;

    ListeningContext(Function<Guild, List<CachedMember>> getListeners) {

        this.getListeners = getListeners;
    }

    public List<CachedMember> getListeners() {
        return getListeners.apply(Radio.getInstance().getGuild());
    }

    public boolean hasListener(String id) {
        return getListeners().stream().anyMatch(m -> m.getId().equals(id));
    }

    public boolean hasListener(ISnowflake user) {
        return hasListener(user.getId());
    }

    public boolean hasListener(ICached<ISnowflake> user) {
        return hasListener(user.getId());
    }

    public static class Utils {
        public static final Function<VoiceChannel, List<CachedMember>> ACTIVE_LISTENERS = vc -> vc == null ? Collections.emptyList() : vc.getMembers().stream().filter(m -> !m.getUser().isBot() && m.getVoiceState() != null && !m.getVoiceState().isDeafened()).map(CachedMember::new).collect(Collectors.toList());

    }
}
