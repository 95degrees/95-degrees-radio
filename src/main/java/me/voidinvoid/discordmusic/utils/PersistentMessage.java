package me.voidinvoid.discordmusic.utils;

import me.voidinvoid.discordmusic.utils.cache.CachedChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * DiscordMusic - 22/07/2020
 * This code was developed by VoidInVoid / Exfusion
 * © 2020
 */

public class PersistentMessage {

    private String id;
    private final CachedChannel<TextChannel> channel;
    private Message messageContent;

    public PersistentMessage(Message message) {

        this.id = message.getId();
        this.channel = new CachedChannel<>(message.getTextChannel());
        this.messageContent = message;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Message getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(Message messageContent) {
        this.messageContent = messageContent;
    }

    public CachedChannel<TextChannel> getChannel() {
        return channel;
    }
}
