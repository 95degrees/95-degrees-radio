package me.voidinvoid.discordmusic.utils.reactions;

import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.EventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MessageReactionCallbackManager implements EventListener {

    private Map<String, Consumer<MessageReactionAddEvent>> callbacks = new HashMap<>();

    public void registerCallback(String id, Consumer<MessageReactionAddEvent> callback) {
        callbacks.put(id, callback);
    }

    public void removeCallback(String id) {
        callbacks.remove(id);
    }

    @Override
    public void onEvent(Event ev) {
        if (ev instanceof MessageReactionAddEvent) {
            MessageReactionAddEvent e = (MessageReactionAddEvent) ev;

            if (e.getUser().getId().equals(e.getJDA().getSelfUser().getId())) return;

            Consumer<MessageReactionAddEvent> cb = callbacks.get(e.getMessageId());

            if (cb == null) return;
            e.getReaction().removeReaction(e.getUser()).queue();
            cb.accept(e);
        }
    }
}
