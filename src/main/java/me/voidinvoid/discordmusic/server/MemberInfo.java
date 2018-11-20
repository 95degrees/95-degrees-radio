package me.voidinvoid.discordmusic.server;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

public class MemberInfo {

    public String name;
    public String identifier;
    public String internalId;
    public String avatarUrl;

    public MemberInfo(String name, String identifier, String internalId) {

        this.name = name;
        this.identifier = identifier;
        this.internalId = internalId;
    }

    public MemberInfo(Member member) {
        User user = member.getUser();

        name = member.getEffectiveName();
        identifier = user.getDiscriminator();
        internalId = user.getId();
        avatarUrl = user.getAvatarUrl();
    }
}
