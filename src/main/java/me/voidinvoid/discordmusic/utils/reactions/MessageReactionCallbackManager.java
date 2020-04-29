package me.voidinvoid.discordmusic.utils.reactions;

import me.voidinvoid.discordmusic.RadioService;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.EventListener;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MessageReactionCallbackManager implements RadioService, EventListener {

    private Map<String, Consumer<MessageReactionAddEvent>> callbacks = new HashMap<>();
    private List<MessageReactionListener> reactionListeners = new ArrayList<>();

    protected MessageReactionCallbackManager listen(MessageReactionListener listener) { //protected since message reaction listeners auto-register themselves here
        reactionListeners.add(listener);

        return this;
    }

    public void removeAllListeners(Message message) {
        var id = message.getId();

        removeAllListeners(id);
    }

    public void removeAllListeners(String messageId) {
        reactionListeners.removeIf(m -> messageId.equals(m.messageId));
    }

    public void registerCallback(String id, Consumer<MessageReactionAddEvent> callback) {
        callbacks.put(id, callback);
    }

    public void removeCallback(String id) {
        callbacks.remove(id);
    }

    @Override
    public void onEvent(@Nonnull GenericEvent ev) {
        if (ev instanceof MessageReactionAddEvent) {
            MessageReactionAddEvent e = (MessageReactionAddEvent) ev;

            if (e.getUser() == null || e.getUser().getId().equals(e.getJDA().getSelfUser().getId())) return;

            var id = e.getMessageId();
            var emote = e.getReaction().getReactionEmote().getName();

            boolean reactionRemoved = false;

            for (var listener : reactionListeners) {
                if (id.equals(listener.messageId)) {

                    for (var handler : listener.handlers.entrySet()) {
                        if (handler.getKey().equals(emote)) {
                            handler.getValue().accept(e.getMember());

                            e.getReaction().removeReaction(e.getUser()).queue();
                            reactionRemoved = true;
                        }
                    }
                }
            }

            Consumer<MessageReactionAddEvent> cb = callbacks.get(id);

            if (cb == null) return;
            cb.accept(e);

            if (!reactionRemoved) {
                e.getReaction().removeReaction(e.getUser()).queue();
            }
        } else if (ev instanceof MessageDeleteEvent) {
            MessageDeleteEvent e = (MessageDeleteEvent) ev;

            var id = e.getMessageId();

            removeAllListeners(id);
            callbacks.remove(id);
        }
    }
}
