package me.voidinvoid.discordmusic.levelling;

import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Colors;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.guild.voice.*;
import net.dv8tion.jda.core.hooks.EventListener;
import org.bson.Document;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;

public class LevellingManager implements EventListener {

    private ScheduledExecutorService executor;
    private Map<String, ScheduledFuture> listeningTracker = new HashMap<>();
    private DatabaseManager databaseManager;

    public LevellingManager() {
        executor = Executors.newScheduledThreadPool(1);
        databaseManager = Radio.getInstance().getService(DatabaseManager.class);

        VoiceChannel v = Radio.getInstance().getJda().getVoiceChannelById(RadioConfig.config.channels.voice);
        v.getMembers().forEach(m -> trackIfEligible(m.getUser(), v, m.getVoiceState().isDeafened()));
    }

    @Override
    public void onEvent(Event ev) {
        if (ev instanceof GuildVoiceJoinEvent) {
            trackIfEligible(((GuildVoiceJoinEvent) ev).getMember().getUser(), ((GuildVoiceJoinEvent) ev).getChannelJoined(), ((GenericGuildVoiceEvent) ev).getVoiceState().isDeafened());
        } else if (ev instanceof GuildVoiceLeaveEvent) {
            trackIfEligible(((GuildVoiceLeaveEvent) ev).getMember().getUser(), ((GuildVoiceLeaveEvent) ev).getChannelLeft(), ((GenericGuildVoiceEvent) ev).getVoiceState().isDeafened());
        } else if (ev instanceof GuildVoiceMoveEvent) {
            trackIfEligible(((GuildVoiceMoveEvent) ev).getMember().getUser(), ((GuildVoiceMoveEvent) ev).getChannelJoined(), ((GenericGuildVoiceEvent) ev).getVoiceState().isDeafened());
        } else if (ev instanceof GuildVoiceDeafenEvent) {
            trackIfEligible(((GuildVoiceDeafenEvent) ev).getMember().getUser(), ((GuildVoiceDeafenEvent) ev).getVoiceState().getChannel(), ((GuildVoiceDeafenEvent) ev).isDeafened());
        }
    }

    private void trackIfEligible(User user, VoiceChannel channel, boolean deafened) {

        if (user.isBot()) return;

        if (ChannelScope.RADIO_VOICE.check(channel) && !deafened) {
            if (!listeningTracker.containsKey(user.getId())) {
                listeningTracker.put(user.getId(), track(user));
            }
        } else {
            ScheduledFuture s = listeningTracker.get(user.getId());
            if (s != null) {
                System.out.println("LEVELLING: CANCELLED TRACKING " + user);
                s.cancel(false);
            }
        }
    }

    private ScheduledFuture track(User user) {
        System.out.println("LEVELLING: TRACKING " + user);
        String id = user.getId();
        return executor.scheduleAtFixedRate(() -> {
            databaseManager.getCollection("users").updateOne(eq("_id", id), new Document("$inc", new Document("total_listen_time", 1)));
            rewardExperience(user, 1); //TODO
        }, 0, 1, TimeUnit.MINUTES);
    }

    public void rewardExperience(User user, int amount) {
        TextChannel c = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat);

        c.sendMessage(new EmbedBuilder()
                .setTitle("Level Up")
                .setColor(Colors.ACCENT_LEVEL_UP)
                .setThumbnail("https://cdn.discordapp.com/attachments/505174503752728597/537703976032796712/todo.png")
                .setDescription(user.getAsMention() + " has levelled up!\n3 ➠ **4**")
                .addField("Reward", "<:degreecoin:431982714212843521> ?", false)
                .setTimestamp(OffsetDateTime.now())
                .setFooter(user.getName(), user.getAvatarUrl())
                .build()
        ).queue();
    }
}
