package me.voidinvoid.discordmusic.utils.cache;

import me.voidinvoid.discordmusic.Radio;
import net.dv8tion.jda.api.entities.Guild;

import java.util.Objects;

public class CachedGuild implements ICached<Guild> {

    private String guildId;

    public CachedGuild(String guildId) {

        this.guildId = guildId;
    }

    public CachedGuild(Guild guild) {

        this.guildId = guild.getId();
    }

    public String getId() {
        return guildId;
    }

    public Guild get() {
        return Radio.getInstance().getJda().getGuildById(guildId);
    }

    @Override
    public boolean is(Guild item) {
        return item != null && item.getId().equals(guildId);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CachedGuild && Objects.equals(guildId, ((CachedGuild) obj).guildId);
    }
}
