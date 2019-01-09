package me.voidinvoid.discordmusic.tasks.types;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.SongOrchestrator;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.tasks.ParameterList;
import me.voidinvoid.discordmusic.tasks.RadioTaskExecutor;
import me.voidinvoid.discordmusic.utils.Colors;
import me.voidinvoid.discordmusic.utils.reactions.MessageReactionCallbackManager;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AnnounceTask extends RadioTaskExecutor {

    private static final String EVENT_SUBSCRIBE_REACTION = "🔔";

    private Map<String, String> messageEventIds = new HashMap<>();

    @Override
    public void runTask(SongOrchestrator orch, ParameterList params) {
        String message = params.get("message", String.class);
        boolean announceToDj = params.get("announce_to_dj_channel", Boolean.class);
        boolean announceToText = params.get("announce_to_text_channel", Boolean.class);
        String title = params.get("title", String.class);
        String additionalChannel = params.get("announce_to_channel", String.class);
        int colour = params.get("colour", Integer.class);
        long deleteAfter = params.get("delete_after", Long.class);
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
    }

    private void createSubscribeLink(Message message, String eventId) {
        message.addReaction(EVENT_SUBSCRIBE_REACTION).queue();
        messageEventIds.put(message.getId(), eventId);

        MessageReactionCallbackManager callbacks = Radio.getInstance().getService(MessageReactionCallbackManager.class);

        callbacks.registerCallback(message.getId(), e -> {
            e.getUser().openPrivateChannel().queue(c -> c.sendMessage(
                    new EmbedBuilder()
                            .setTitle("Event Subscription")
                            .setDescription("🔔 You're now subscribed to **" + eventId + "** and you will be notified when this event happens in the future")
                            .setFooter("95 Degrees Radio", e.getJDA().getSelfUser().getAvatarUrl())
                            .setTimestamp(OffsetDateTime.now())
                            .setColor(Colors.ACCENT_EVENT_SUBSCRIPTION)
                            .build()).queue());
            //TODO subscribe action here
        });
    }
}
