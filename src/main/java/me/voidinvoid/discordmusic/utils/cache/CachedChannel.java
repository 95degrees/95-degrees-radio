package me.voidinvoid.discordmusic.utils.cache;

import me.voidinvoid.discordmusic.Radio;
import net.dv8tion.jda.api.entities.GuildChannel;

import java.util.Objects;

@SuppressWarnings("unchecked")
public class CachedChannel<T extends GuildChannel> implements ICached<GuildChannel> {

    private String channelId;

    public CachedChannel(String channelId) {

        this.channelId = channelId;
    }

    public CachedChannel(T channel) {

        this.channelId = channel.getId();
    }

    public String getId() {
        return channelId;
    }

    public T get() {
        return (T) Radio.getInstance().getJda().getGuildChannelById(channelId);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CachedChannel && Objects.equals(channelId, ((CachedChannel<T>) obj).channelId);
    }
}
