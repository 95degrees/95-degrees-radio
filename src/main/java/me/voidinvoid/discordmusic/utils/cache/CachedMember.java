package me.voidinvoid.discordmusic.utils.cache;

import me.voidinvoid.discordmusic.Radio;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

import java.util.Objects;

public class CachedMember implements ICached<Member> {

    private String memberId;

    public CachedMember(String memberId) {

        this.memberId = memberId;
    }

    public CachedMember(User user) {

        this.memberId = user.getId();
    }

    public CachedMember(Member member) {

        this.memberId = member.getUser().getId();
    }

    public String getId() {
        return memberId;
    }

    public Member get() {
        return Radio.getInstance().getGuild().retrieveMemberById(memberId).onErrorMap(m -> null).complete();
    }

    @Override
    public boolean is(Member item) {
        return item != null && item.getId().equals(memberId);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CachedMember && Objects.equals(memberId, ((CachedMember) obj).memberId);
    }
}
