package me.voidinvoid.discordmusic.utils.reactions;

import me.voidinvoid.discordmusic.utils.Service;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ReactionListener {

    private Message message;
    private Map<String, Consumer<ReactionEvent>> callbacks = new HashMap<>();

    private boolean autoCancel = true;

    public ReactionListener(Message message) {

        this.message = message;

        var cb = Service.of(MessageReactionCallbackManager.class);
        cb.registerCallback(message.getId(), this::onReact);
    }

    public ReactionListener(Message message, boolean autoCancel) {
        this(message);
        this.autoCancel = autoCancel;
    }

    private void onReact(MessageReactionAddEvent e) {

        var reaction = e.getReaction().getReactionEmote().getName();

        var cb = callbacks.get(reaction);

        if (cb != null) {
            var ev = new ReactionEvent(e);
            ev.setCancelled(autoCancel);
            cb.accept(ev);

            if (ev.isCancelled() && !(e.getChannel() instanceof PrivateChannel)) { //remove their reaction
                e.getReaction().removeReaction(e.getUser()).queue();
            }
        }
    }

    public ReactionListener add(String reaction, Consumer<ReactionEvent> callback) {
        String guessedEmoteId = null;

        reaction = reaction.trim();

        System.out.println("REACTION: '" + reaction + "'");

        if (reaction.contains(":")) { //probably a custom emoji
            var colonSplit = reaction.split(":");
            System.out.println("split: " + colonSplit.length);
            System.out.println(String.join(", ", colonSplit));

            if (colonSplit.length == 3) {
                var id = colonSplit[2];
                id = id.substring(0, id.length() - 1);

                guessedEmoteId = id;
            }
        } else if (reaction.endsWith(">") && reaction.length() == 19) {
            guessedEmoteId = reaction.substring(0, 18);
        }

        if (guessedEmoteId != null) {
            System.out.println("id: " + guessedEmoteId);

            try {
                Emote emote = message.getGuild().getEmoteById(guessedEmoteId);
                if (emote != null) {
                    return add(emote, callback);
                }
            } catch (Exception ignored) {
            }
        }

        message.addReaction(reaction).queue();
        callbacks.put(reaction, callback);

        return this;
    }

    public ReactionListener add(Emote reaction, Consumer<ReactionEvent> callback) {
        message.addReaction(reaction).queue();
        callbacks.put(reaction.getName(), callback);

        return this;
    }

    public ReactionListener remove(String reaction) {
        callbacks.remove(reaction);

        return this;
    }

    public ReactionListener remove(Emote reaction) {
        callbacks.remove(reaction.getName());

        return this;
    }

    public static class ReactionEvent {

        private final Member member;
        private final MessageReactionAddEvent internalEvent;
        private boolean cancelled;

        ReactionEvent(MessageReactionAddEvent internalEvent) {

            this.member = internalEvent.getChannelType() == ChannelType.PRIVATE ? null : internalEvent.getMember();
            this.internalEvent = internalEvent;
        }

        public Member getMember() {
            return member;
        }

        public MessageReactionAddEvent getInternalEvent() {
            return internalEvent;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }
}
