package me.voidinvoid.discordmusic.tasks.types;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.SongOrchestrator;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.tasks.ParameterList;
import me.voidinvoid.discordmusic.tasks.RadioTaskExecutor;
import me.voidinvoid.discordmusic.utils.Colors;
import me.voidinvoid.discordmusic.utils.reactions.MessageReactionCallbackManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.mongodb.client.model.Filters.eq;

public class AnnounceTask extends RadioTaskExecutor {

    private static final String EVENT_SUBSCRIBE_REACTION = "🔔";

    private Map<String, Long> lastSubscriptionAttempt = new HashMap<>();

    @Override
    public void runTask(SongOrchestrator orch, ParameterList params) {
        String message = params.get("message", String.class);
        boolean announceToDj = params.get("announce_to_dj_channel", Boolean.class);
        boolean announceToText = params.get("announce_to_text_channel", Boolean.class);
        String title = params.get("title", String.class);
        String additionalChannel = params.get("announce_to_channel", String.class);
        int colour = params.get("colour", Integer.class);
        long deleteAfter = params.get("delete_after", Integer.class);
        String image = params.get("image_url", String.class);
        String eventId = params.get("event_subscription_id", String.class);

        MessageEmbed embed = new EmbedBuilder()
                .setTitle(title)
                .setDescription(message)
                .setImage(image)
                .setTimestamp(OffsetDateTime.now())
                .setColor(colour)
                .setFooter(eventId == null ? null : "Click the bell to subscribe to this event", null)
                .build();

        if (announceToDj)
            Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.djChat).sendMessage(embed).queue(m -> {
                if (eventId != null) {
                    createSubscribeLink(m, eventId);
                }
            });

        if (announceToText)
            Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat).sendMessage(embed).queue(m -> {
                if (eventId != null) {
                    createSubscribeLink(m, eventId);
                }
            });

        if (additionalChannel != null) {
            TextChannel channel = Radio.getInstance().getJda().getTextChannelById(additionalChannel);

            if (channel != null) {
                channel.sendMessage(embed).queue(m -> {
                    if (deleteAfter > 0) {
                        m.delete().queueAfter(deleteAfter, TimeUnit.SECONDS);
                    }

                    if (eventId != null) {
                        createSubscribeLink(m, eventId);
                    }
                });
            }
        }

        if (eventId != null) {
            DatabaseManager db = Radio.getInstance().getService(DatabaseManager.class);

            MessageEmbed eventPrivateMessage = new EmbedBuilder()
                    .setTitle(title)
                    .setDescription(message)
                    .setImage(image)
                    .setTimestamp(OffsetDateTime.now())
                    .setColor(colour)
                    .setFooter("🔔 This is your " + eventId + " subscription reminder", null)
                    .build();

            if (db != null) {
                db.getCollection("users").find(new Document("subscriptions", new Document("$elemMatch", new Document("$eq", eventId)))).forEach((Consumer<? super Document>) d -> {
                    User u = Radio.getInstance().getJda().getUserById(d.getString("_id"));
                    if (u != null) u.openPrivateChannel().queue(c -> c.sendMessage(eventPrivateMessage).queue());
                });
            }
        }
    }

    private void createSubscribeLink(Message message, String eventId) {
        message.addReaction(EVENT_SUBSCRIBE_REACTION).queue();

        MessageReactionCallbackManager callbacks = Radio.getInstance().getService(MessageReactionCallbackManager.class);

        callbacks.registerCallback(message.getId(), e -> {
            DatabaseManager db = Radio.getInstance().getService(DatabaseManager.class);

            if (db != null) {
                if ((System.currentTimeMillis() - lastSubscriptionAttempt.getOrDefault(e.getUser().getId(), 0L)) < 10000L) {
                    return;
                }

                lastSubscriptionAttempt.put(e.getUser().getId(), System.currentTimeMillis());

                final boolean subscribed;

                db.findOrCreateUser(e.getUser(), true);
                MongoCollection users = db.getCollection("users");
                UpdateResult ur = users.updateOne(eq("_id", e.getUser().getId()), new Document("$addToSet", new Document("subscriptions", eventId)));
                if (ur.getModifiedCount() == 0) {
                    subscribed = false;
                    users.updateOne(eq("_id", e.getUser().getId()), new Document("$pull", new Document("subscriptions", eventId)));
                } else {
                    subscribed = true;
                }

                e.getUser().openPrivateChannel().queue(c -> c.sendMessage(
                        new EmbedBuilder()
                                .setTitle("Event Subscription")
                                .setDescription(subscribed ? "🔔 You're now subscribed to **" + eventId + "** and you will be notified when this event happens in the future" : "🔕 You've unsubscribed from **" + eventId + "** and you will no longer be notified when this event happens in the future")
                                .setFooter("95 Degrees Radio", e.getJDA().getSelfUser().getAvatarUrl())
                                .setTimestamp(OffsetDateTime.now())
                                .setColor(Colors.ACCENT_EVENT_SUBSCRIPTION)
                                .build()).queue(),
                        fail -> Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat).sendMessage("⚠ " + e.getUser().getAsMention() + ", the bot could not send you a private message. Make sure you allow private messages in your settings to receive radio notifications").queue());
            }
        });
    }
}
