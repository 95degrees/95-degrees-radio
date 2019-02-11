package me.voidinvoid.discordmusic.levelling;

import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.coins.CoinsServerManager;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.currency.CurrencyManager;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Colors;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.GuildVoiceState;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.guild.voice.*;
import net.dv8tion.jda.core.hooks.EventListener;
import org.bson.Document;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

public class LevellingManager implements RadioService, EventListener {

    private ScheduledExecutorService executor;
    private Map<String, ScheduledFuture> listeningTracker = new HashMap<>();
    private DatabaseManager databaseManager;
    private VoiceChannel voiceChannel;
    private Map<Integer, Level> levels = new HashMap<>();

    @Override
    public void onLoad() {
        databaseManager = Radio.getInstance().getService(DatabaseManager.class);

        Document levelling = databaseManager.getCollection("internal").find(eq("_id", "levelling_config")).first();
        if (levelling != null) {
            Document levels = levelling.get("levels", Document.class);
            levels.keySet().forEach(l -> {
                int level = Integer.parseInt(l);
                Document lvlInfo = levels.get(l, Document.class);

                List<AppliedLevelExtra> extras = new ArrayList<>();

                if (lvlInfo.containsKey("extras")) {
                    Document d = lvlInfo.get("extras", Document.class);
                    d.keySet().forEach(k -> {
                        Object val = d.get(k);
                        try {
                            extras.add(new AppliedLevelExtra(LevelExtras.valueOf(k), val));
                        } catch (Exception e) {
                            log("LEVELLING: unknown extra: " + k);
                            e.printStackTrace();
                        }
                    });
                }

                this.levels.put(level, new Level(level, lvlInfo.getInteger("required"), lvlInfo.getInteger("reward"), extras));
            });
        }

        if (this.executor == null) {
            executor = Executors.newScheduledThreadPool(1);

            voiceChannel = Radio.getInstance().getJda().getVoiceChannelById(RadioConfig.config.channels.voice);
            voiceChannel.getMembers().forEach(m -> trackIfEligible(m.getUser(), voiceChannel, m.getVoiceState().isDeafened()));
        }
    }

    @Override
    public void onEvent(Event ev) {
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

    private void trackIfEligible(User user, VoiceChannel channel, boolean deafened) {

        if (user.isBot()) return;

        if (channel != null && ChannelScope.RADIO_VOICE.check(channel) && !deafened) {
            if (!listeningTracker.containsKey(user.getId())) {
                listeningTracker.put(user.getId(), track(user));
            }
        } else {
            ScheduledFuture s = listeningTracker.remove(user.getId());
            if (s != null) {
                log("LEVELLING: CANCELLED TRACKING " + user);
                s.cancel(false);
            }
        }
    }

    private ScheduledFuture track(User user) {
        log("LEVELLING: TRACKING " + user);
        String id = user.getId();
        return executor.scheduleAtFixedRate(() -> {
            GuildVoiceState v = voiceChannel.getGuild().getMember(user).getVoiceState();
            if (v == null || !v.inVoiceChannel() || !ChannelScope.RADIO_VOICE.check(v.getChannel()) || v.isDeafened()) {
                log("LEVELLING: desync of " + user);
                return;
            }
            databaseManager.getCollection("users").updateOne(eq("_id", id), new Document("$inc", new Document("total_listen_time", 1)));
            rewardExperience(user, 1); //TODO
        }, 1, 1, TimeUnit.MINUTES);
    }

    public AppliedLevelExtra getLatestExtra(int experience, LevelExtras extra) {
        int lvl = calculateLevel(experience).getLevel();
        log("lvl: " + lvl + ", xp: " + experience);
        for (int i = lvl; i >= 1; i--) { //loop backwards through levels to find first instance of the extra
            log("lvl: " + lvl + ", i: " + i);
            for (AppliedLevelExtra a : levels.get(i).getExtras()) {
                log("latest " + a.getExtra() + " extra for user is " + a.getValue());
                if (a.getExtra().equals(extra)) return a;
            }
        }

        log("!!");

        return new AppliedLevelExtra(extra, extra.getOriginalValue());
    }

    public AppliedLevelExtra getLatestExtra(User u, LevelExtras extra) {
        Document d = databaseManager.findOrCreateUser(u, true);

        int xp = d.getInteger("total_experience", 0);
        log("!!!!!! " + xp);

        return getLatestExtra(xp, extra);
    }

    public Level getLevel(User user) {
        return calculateLevel(getExperience(user));
    }

    public Level getNextLevel(Level level) {
        int lvl = level.getLevel();

        return levels.get(lvl + 1);
    }

    public Level calculateLevel(int experience) {
        int ct = 0;
        int lvl = 0;
        Level cl = levels.get(1);

        while (experience > ct) {
            Level l = levels.get(++lvl);
            if (ct + l.getRequiredXp() > experience) return cl;

            cl = l;
            ct += cl.getRequiredXp();
        }

        return cl;
    }

    public int getExperienceRequired(int experience) { //gets the xp required to level up
        int ct = 0;
        int lvl = 0;
        Level cl;

        while (experience > ct) {
            Level l = levels.get(++lvl);
            if (ct + l.getRequiredXp() > experience) return ct + l.getRequiredXp() - experience;

            cl = l;
            ct += cl.getRequiredXp();
        }

        return levels.get(1).getRequiredXp();
    }

    public int getExperience(User user) {
        var doc = databaseManager.getCollection("users").find(eq(user.getId())).first();

        if (doc == null) return 0;

        return doc.getInteger("total_experience", 0);
    }

    public void rewardExperience(User user, int amount) {
        Document doc = databaseManager.getCollection("users").findOneAndUpdate(eq("_id", user.getId()), new Document("$inc", new Document("total_experience", amount)));

        if (doc == null) {
            log(user + "'s db document is null");
            return;
        }

        int xp = doc.getInteger("total_experience", 0);

        Level prevLevel = calculateLevel(xp);
        Level currentLevel = calculateLevel(xp + amount);

        if (prevLevel.getLevel() < currentLevel.getLevel()) {
            List<AppliedLevelExtra> unlockedExtras = new ArrayList<>();
            int reward = 0;

            for (int i = prevLevel.getLevel() + 1; i <= currentLevel.getLevel(); i++) {
                Level l = levels.get(i);
                reward += l.getReward();
                unlockedExtras.addAll(l.getExtras());
            }

            if (reward >= 0) CoinsServerManager.addCredit(user, reward);
            //todo make sure that its only given once if the config is changed

            TextChannel c = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat);

            c.sendMessage("🎉 " + user.getAsMention()).queue();
            c.sendMessage(new EmbedBuilder()
                    .setTitle("Level Up")
                    .setColor(Colors.ACCENT_LEVEL_UP)
                    .setThumbnail(RadioConfig.config.images.levellingUpLogo)
                    .setDescription(user.getAsMention() + " has levelled up for listening to the radio!\nLevel " + prevLevel.getLevel() + " ➠ **" + currentLevel.getLevel() + "**")
                    .addField("Reward", CurrencyManager.DEGREECOIN_EMOTE + " " + reward
                            + (unlockedExtras.isEmpty() ? "" : "\n" + unlockedExtras.stream().map(a -> a.getExtra().getDisplayName() + " " + a.getExtra().formatParameter(getLatestExtra(xp, a.getExtra()).getValue()) + " ➠ **" + a.getExtra().formatParameter(a.getValue()) + "**").collect(Collectors.joining("\n"))), false)
                    .setTimestamp(OffsetDateTime.now())
                    .setFooter(user.getName(), user.getAvatarUrl())
                    .build()
            ).queue();
        }
    }
}
