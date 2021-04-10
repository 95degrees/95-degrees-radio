package me.voidinvoid.discordmusic.levelling;

import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.activity.ListeningContext;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.guardian.GuardianIntegrationManager;
import me.voidinvoid.discordmusic.stats.Statistic;
import me.voidinvoid.discordmusic.stats.UserStatisticsManager;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Service;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.voice.*;
import net.dv8tion.jda.api.hooks.EventListener;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ListeningTrackerManager implements RadioService, EventListener {

    private ScheduledExecutorService executor;
    private Map<String, ScheduledFuture<?>> listeningTracker = new HashMap<>();
    private Map<String, Integer> currentListeningDurations = new HashMap<>();

    @Override
    public void onLoad() {
        if (this.executor == null) {
            executor = Executors.newScheduledThreadPool(1);

            ListeningContext.ALL.getListeners().forEach(m -> trackIfEligible(m.get().getUser(), m.get().getVoiceState().getChannel(), false));
        }
    }

    public int getCurrentListeningDuration(Member member) {
        return currentListeningDurations.getOrDefault(member.getId(), 0);
    }

    public void resetCurrentListeningDurations() {
        currentListeningDurations.clear();
        log("Reset current listening durations");
    }

    @Override
    public void onEvent(@Nonnull GenericEvent ev) {
        if (ev instanceof GuildVoiceJoinEvent) {
            trackIfEligible(((GuildVoiceJoinEvent) ev).getMember().getUser(), ((GuildVoiceJoinEvent) ev).getChannelJoined(), ((GenericGuildVoiceEvent) ev).getVoiceState().isDeafened());
        } else if (ev instanceof GuildVoiceLeaveEvent) {
            trackIfEligible(((GuildVoiceLeaveEvent) ev).getMember().getUser(), null, ((GenericGuildVoiceEvent) ev).getVoiceState().isDeafened());
        } else if (ev instanceof GuildVoiceMoveEvent) {
            trackIfEligible(((GuildVoiceMoveEvent) ev).getMember().getUser(), ((GuildVoiceMoveEvent) ev).getChannelJoined(), ((GenericGuildVoiceEvent) ev).getVoiceState().isDeafened());
        } else if (ev instanceof GuildVoiceDeafenEvent) {
            trackIfEligible(((GuildVoiceDeafenEvent) ev).getMember().getUser(), ((GuildVoiceDeafenEvent) ev).getVoiceState().getChannel(), ((GuildVoiceDeafenEvent) ev).isDeafened());
        }
    }

    public void trackIfEligible(User user, VoiceChannel channel, boolean deafened) {

        if (user.isBot()) return;

        if (channel != null && ChannelScope.RADIO_VOICE.check(channel)) {
            if (deafened) {
                Service.of(AchievementManager.class).rewardAchievement(user, Achievement.MUTE_RADIO);
            } else if (!listeningTracker.containsKey(user.getId())) {
                listeningTracker.put(user.getId(), track(user));
                return;
            }
        }

        ScheduledFuture<?> s = listeningTracker.remove(user.getId());
        if (s != null) {
            log("Cancelled tracking " + user);
            s.cancel(false);
        }
    }

    private ScheduledFuture<?> track(User user) {
        log("Tracking " + user);
        Service.of(DatabaseManager.class).findOrCreateUser(user, true); //ensure it's created

        return executor.scheduleAtFixedRate(new Runnable() {
            int mins;

            @Override
            public void run() {
                log("Updating tracking info....");
                try {
                    var member = Radio.getInstance().getGuild().getMember(user);

                    if (member == null) {
                        warn("Couldn't find " + user);
                        return;
                    }

                    var v = member.getVoiceState();

                    if (v == null || !v.inVoiceChannel() || v.isDeafened() || !ChannelScope.RADIO_VOICE.check(v.getChannel())) {
                        warn("Desync of " + user);
                        return;
                    }

                    var stats = Service.of(UserStatisticsManager.class);

                    stats.addStatistic(user, Statistic.LISTEN_TIME, 1);

                    if (stats.getTotal(user, Statistic.LISTEN_TIME) >= 600) { //10 hours TODO
                        Radio.getInstance().getService(AchievementManager.class).rewardAchievement(user, Achievement.LISTEN_FOR_10_HOURS);
                    }

                    mins++;
                    currentListeningDurations.merge(member.getId(), 1, Integer::sum);
                    log("Added 1 min to current listening duration for " + user);

                    if (mins % 3 == 0 && mins < 60) {
                        ListeningTrackerManager.this.rewardExperience(user, 1);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    public void rewardExperience(User user, int amount) {
        Radio.getInstance().getService(GuardianIntegrationManager.class).addGuardianExperience(user.getId(), amount, RadioConfig.config.channels.radioChat);
    }
}
