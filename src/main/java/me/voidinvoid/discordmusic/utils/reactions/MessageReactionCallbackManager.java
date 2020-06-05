package me.voidinvoid.discordmusic.utils.reactions;

import me.voidinvoid.discordmusic.RadioService;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MessageReactionCallbackManager implements RadioService, EventListener {

    private Map<String, Consumer<MessageReactionAddEvent>> callbacks = new HashMap<>();

    public void registerCallback(String id, Consumer<MessageReactionAddEvent> callback) {
        callbacks.put(id, callback);
    }

    public void removeCallback(String id) {
        callbacks.remove(id);
    }

    @Override
    public void onEvent(@NotNull GenericEvent ev) {
        if (ev instanceof MessageReactionAddEvent) {
            MessageReactionAddEvent e = (MessageReactionAddEvent) ev;

            if (e.getUser().getId().equals(e.getJDA().getSelfUser().getId())) return;

            Consumer<MessageReactionAddEvent> cb = callbacks.get(e.getMessageId());

            if (cb == null) return;
            if (!(e.getChannel() instanceof PrivateChannel)) e.getReaction().removeReaction(e.getUser()).queue();
            cb.accept(e);
        }
    }
}
