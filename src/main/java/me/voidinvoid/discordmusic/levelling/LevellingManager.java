package me.voidinvoid.discordmusic.levelling;

import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Colors;
import net.dv8tion.jda.core.EmbedBuilder;
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
                            System.out.println("LEVELLING: unknown extra: " + k);
                            e.printStackTrace();
                        }
                    });
                }

                this.levels.put(level, new Level(level, lvlInfo.getInteger("required"), lvlInfo.getInteger("reward"), extras));
            });
        }

        if (this.executor == null) {
            executor = Executors.newScheduledThreadPool(1);

            VoiceChannel v = Radio.getInstance().getJda().getVoiceChannelById(RadioConfig.config.channels.voice);
            v.getMembers().forEach(m -> trackIfEligible(m.getUser(), v, m.getVoiceState().isDeafened()));
        }
    }

    @Override
    public void onEvent(Event ev) {
        if (ev instanceof GuildVoiceJoinEvent) {
            trackIfEligible(((GuildVoiceJoinEvent) ev).getMember().getUser(), ((GuildVoiceJoinEvent) ev).getChannelJoined(), ((GenericGuildVoiceEvent) ev).getVoiceState().isDeafened());
        } else if (ev instanceof GuildVoiceLeaveEvent) {
            trackIfEligible(((GuildVoiceLeaveEvent) ev).getMember().getUser(), ((GuildVoiceLeaveEvent) ev).getVoiceState().getChannel(), ((GenericGuildVoiceEvent) ev).getVoiceState().isDeafened());
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

    public AppliedLevelExtra getLatestExtra(User u, LevelExtras extra) {
        Document d = databaseManager.findOrCreateUser(u, true);

        int xp = d.getInteger("total_experience", 0);

        int lvl = calculateLevel(xp).getLevel();

        for (int i = lvl; i >= 1; i--) { //loop backwards through levels to find first instance of the extra
            for (AppliedLevelExtra a : levels.get(i).getExtras()) {
                if (a.getExtra().equals(extra)) return a;
            }
        }

        return null;
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

    public void rewardExperience(User user, int amount) {
        Document doc = databaseManager.getCollection("users").findOneAndUpdate(eq("_id", user.getId()), new Document("$inc", new Document("total_experience", 1)));

        if (doc == null) {
            System.out.println(user + "'s db document is null");
            return;
        }

        int xp = doc.getInteger("total_experience", 0);

        TextChannel cdebug = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat);

        Level prevLevel = calculateLevel(xp);
        Level currentLevel = calculateLevel(xp + amount);

        cdebug.sendMessage("prev xp: " + xp + "\ncurrent xp: " + (xp + amount)).queue();
        cdebug.sendMessage("prev lvl: " + prevLevel.getLevel() + "\ncurrent lvl: " + currentLevel.getLevel()).queue();

        if (prevLevel.getLevel() < currentLevel.getLevel()) {
            List<AppliedLevelExtra> unlockedExtras = new ArrayList<>();
            int reward = 0;

            for (int i = prevLevel.getLevel() + 1; i <= currentLevel.getLevel(); i++) {
                Level l = levels.get(i);
                reward += l.getReward();
                unlockedExtras.addAll(l.getExtras());
            }

            TextChannel c = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat);

            c.sendMessage(new EmbedBuilder()
                    .setTitle("Level Up")
                    .setColor(Colors.ACCENT_LEVEL_UP)
                    .setThumbnail("https://cdn.discordapp.com/attachments/505174503752728597/537703976032796712/todo.png")
                    .setDescription(user.getAsMention() + " has levelled up!\n" + prevLevel.getLevel() + " ➠ **" + currentLevel.getLevel() + "**")
                    .addField("Reward", "<:degreecoin:431982714212843521> " + reward
                            + (unlockedExtras.isEmpty() ? "" : "\nTODO:" + unlockedExtras.stream().map(a -> a.getExtra().getDisplayName() + " ? ➠ **" + a.getExtra().formatParameter(a.getValue()) + "**").collect(Collectors.joining("\n"))), false)
                    .setTimestamp(OffsetDateTime.now())
                    .setFooter(user.getName(), user.getAvatarUrl())
                    .build()
            ).queue();
        } else {
            TextChannel c = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat);

            c.sendMessage(new EmbedBuilder()
                    .setTitle("Levelling Debug")
                    .setColor(Colors.ACCENT_LEVEL_UP)
                    .setDescription("Prev level: " + prevLevel.getLevel() + ", next lvl requirement: " + levels.get(prevLevel.getLevel() + 1).getRequiredXp() + " xp required. total xp: " + (xp + amount))
                    .setTimestamp(OffsetDateTime.now())
                    .setFooter(user.getName(), user.getAvatarUrl())
                    .build()
            ).queue();
        }
    }
}
