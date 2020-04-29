package me.voidinvoid.discordmusic.utils.cache;

import me.voidinvoid.discordmusic.Radio;
import net.dv8tion.jda.api.entities.Guild;

public class CachedGuild implements ICached {

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
}
