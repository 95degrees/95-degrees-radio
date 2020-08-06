package me.voidinvoid.discordmusic.utils;

import me.voidinvoid.discordmusic.config.RadioConfig;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

import java.util.function.Function;

/**
 * DiscordMusic - 19/07/2020
 * This code was developed by VoidInVoid / Exfusion
 * © 2020
 */

public enum Rank {

    NORMAL(1, m -> true),
    DJ(2, m -> {
        var channel = m.getGuild().getTextChannelById(RadioConfig.config.channels.djChat);
        return (channel != null && channel.canTalk(m));
    }),
    STAFF(3, m -> m.hasPermission(Permission.BAN_MEMBERS));

    private int permissionLevel;
    private final Function<Member, Boolean> hasRank;

    Rank(int permissionLevel, Function<Member, Boolean> hasRank) {

        this.permissionLevel = permissionLevel;
        this.hasRank = hasRank;
    }

    public static Rank get(Member mb) {
        Rank highest = null;

        for (var rank : values()) { //todo optimise, e.g. in reverse
            if ((highest == null || rank.permissionLevel > highest.permissionLevel) && rank.hasRank(mb)) {
                highest = rank;
            }
        }

        return highest;
    }

    public int getPermissionLevel() {
        return permissionLevel;
    }

    public boolean hasRank(Member member) {

        return hasRank.apply(member);
    }
}
