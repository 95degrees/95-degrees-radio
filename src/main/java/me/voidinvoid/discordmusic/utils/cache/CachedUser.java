package me.voidinvoid.discordmusic.utils.cache;

import me.voidinvoid.discordmusic.Radio;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

import java.util.Objects;

public class CachedUser implements ICached<User> {

    private String userId;

    public CachedUser(String userId) {

        this.userId = userId;
    }

    public CachedUser(User user) {

        this.userId = user.getId();
    }

    public CachedUser(Member member) {

        this.userId = member.getUser().getId();
    }

    public String getId() {
        return userId;
    }

    public User get() {
        return Radio.getInstance().getJda().getUserById(userId);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CachedUser && Objects.equals(userId, ((CachedUser) obj).userId);
    }
}
