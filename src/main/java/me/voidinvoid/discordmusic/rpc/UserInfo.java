package me.voidinvoid.discordmusic.rpc;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

public class UserInfo {

    public String name;
    public String identifier;
    public String internalId;
    public String avatarUrl;

    public UserInfo(String name, String identifier, String internalId) {

        this.name = name;
        this.identifier = identifier;
        this.internalId = internalId;
    }

    public UserInfo(Member member) {
        User user = member.getUser();

        name = member.getEffectiveName();
        identifier = user.getDiscriminator();
        internalId = user.getId();
        avatarUrl = user.getEffectiveAvatarUrl();
    }

    public UserInfo(User user) {

        name = user.getName();
        identifier = user.getDiscriminator();
        internalId = user.getId();
        avatarUrl = user.getEffectiveAvatarUrl();
    }
}
