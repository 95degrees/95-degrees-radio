package me.voidinvoid.discordmusic.utils;

import me.voidinvoid.discordmusic.RadioService;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * DiscordMusic - 22/07/2020
 * This code was developed by VoidInVoid / Exfusion
 * © 2020
 */

public class PersistentMessageManager implements RadioService, EventListener {

    private List<PersistentMessage> persistentMessages;

    @Override
    public void onLoad() {
        persistentMessages = new ArrayList<>();
    }

    @Override
    public void onShutdown() {
        if (persistentMessages != null) {
            persistentMessages.forEach(m -> m.getChannel().get().deleteMessageById(m.getId()).queue());
            persistentMessages.clear();
        }
    }

    public CompletableFuture<PersistentMessage> persist(TextChannel channel, MessageEmbed embed) {
        return persist(channel.sendMessage(embed));
    }

    public CompletableFuture<PersistentMessage> persist(MessageAction action) {
        if (persistentMessages.stream().map(m -> m.getChannel().getId()).anyMatch(c -> c.equals(action.getChannel().getId()))) {
            return null; //can't persist more than 1 message per channel!
        }

        return action.submit().thenApply(m -> {
            var pm = new PersistentMessage(m);
            persistentMessages.add(pm);

            return pm;
        });
    }

    public void editMessage(PersistentMessage originalMessage, MessageEmbed embed) {
        var pm = persistentMessages.stream().filter(m -> m.getId().equals(originalMessage.getId())).findAny().orElse(null);

        if (pm == null) {
            return;
        }

        pm.getChannel().get().editMessageById(pm.getId(), embed).queue(pm::setMessageContent);
    }

    public void deleteMessage(PersistentMessage message) {
        message.getChannel().get().deleteMessageById(message.getId()).queue();

        persistentMessages.removeIf(m -> m.getId().equals(message.getId()));
    }

    @Override
    public void onEvent(@Nonnull GenericEvent ev) {
        if (ev instanceof GuildMessageReceivedEvent) {
            var msg = ((GuildMessageReceivedEvent) ev).getMessage();
            var tc = ((GuildMessageReceivedEvent) ev).getChannel();

            for (var pm : persistentMessages) {
                if (pm.getId().equals(msg.getId())) { //don't do anything when persistent messages are posted (fail-safe)
                    return;
                }

                if (tc.getId().equals(pm.getChannel().getId())) { //we need to move the persistent message for this channel to the bottom
                    tc.deleteMessageById(pm.getId()).queue();
                    var m = tc.sendMessage(pm.getMessageContent()).complete();
                    pm.setId(m.getId());
                    return;
                }
            }
        }
    }
}
