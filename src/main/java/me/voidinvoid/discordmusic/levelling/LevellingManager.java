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
    }

    @Override
    public void onEvent(Event ev) {
        if (ev instanceof GuildVoiceJoinEvent || ev instanceof GuildVoiceLeaveEvent) {
            trackIfEligible(((GenericGuildVoiceEvent) ev).getVoiceState(), ((GenericGuildVoiceEvent) ev).getVoiceState().getChannel(), ((GenericGuildVoiceEvent) ev).getVoiceState().isDeafened());
        } else if (ev instanceof GuildVoiceMoveEvent) {
            trackIfEligible(((GuildVoiceMoveEvent) ev).getVoiceState(), ((GuildVoiceMoveEvent) ev).getChannelJoined(), ((GuildVoiceMoveEvent) ev).getVoiceState().isDeafened());
        } else if (ev instanceof GuildVoiceDeafenEvent) {
            trackIfEligible(((GuildVoiceDeafenEvent) ev).getVoiceState(), ((GuildVoiceDeafenEvent) ev).getVoiceState().getChannel(), ((GuildVoiceDeafenEvent) ev).isDeafened());
        }
    }

    private void trackIfEligible(GuildVoiceState vs, VoiceChannel channel, boolean deafened) {

         User u = vs.getMember().getUser();

        if (vs.inVoiceChannel() && ChannelScope.RADIO_VOICE.check(channel) && deafened) {
            if (!listeningTracker.containsKey(u.getId())) {
                listeningTracker.put(u.getId(), track(u));
            }
        } else {
            ScheduledFuture s = listeningTracker.get(u.getId());
            if (s != null) {
                System.out.println("LEVELLING: CANCELLED TRACKING " + u);
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
                .build()
        ).queue();
    }
}
