package me.voidinvoid.discordmusic.utils.reactions;

import me.voidinvoid.discordmusic.utils.Service;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * DiscordMusic - 28/04/2020
 * This code was developed by VoidInVoid / Exfusion
 * © 2020
 */

public class MessageReactionListener {

    private final Message message; //ideally wouldn't store a message directly, but used for adding reacts automatically
    protected final String messageId;
    protected final Map<String, Consumer<Member>> handlers = new HashMap<>();

    public MessageReactionListener(Message message) {

        this.message = message;
        this.messageId = message.getId();

        Service.of(MessageReactionCallbackManager.class).listen(this);
    }

    public MessageReactionListener on(String emote, Consumer<Member> callback) {
        handlers.put(emote, callback);

        message.addReaction(emote).queue();

        return this;
    }
}
