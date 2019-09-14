package me.voidinvoid.discordmusic.coins;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.currency.CurrencyManager;
import me.voidinvoid.discordmusic.currency.Transaction;
import me.voidinvoid.discordmusic.currency.TransactionType;
import me.voidinvoid.discordmusic.events.SongEventListener;
import me.voidinvoid.discordmusic.levelling.Achievement;
import me.voidinvoid.discordmusic.levelling.AchievementManager;
import me.voidinvoid.discordmusic.rpc.RPCSocketManager;
import me.voidinvoid.discordmusic.songs.Playlist;
import me.voidinvoid.discordmusic.stats.Statistic;
import me.voidinvoid.discordmusic.stats.UserStatisticsManager;
import me.voidinvoid.discordmusic.utils.ConsoleColor;
import me.voidinvoid.discordmusic.utils.Formatting;
import me.voidinvoid.discordmusic.utils.Service;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceDeafenEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.hooks.EventListener;

import javax.annotation.Nonnull;
import java.awt.*;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

public class CoinCreditorManager implements RadioService, EventListener, SongEventListener {

    private VoiceChannel voiceChannel;
    private TextChannel textChannel;

    private Map<String, UserCoinTracker> coinGains = new HashMap<>();

    @Override
    public boolean canRun(RadioConfig config) {
        return config.useCoinGain;
    }

    @Override
    public void onLoad() {

        JDA jda = Radio.getInstance().getJda();
        voiceChannel = jda.getVoiceChannelById(RadioConfig.config.channels.voice);
        textChannel = jda.getTextChannelById(RadioConfig.config.channels.radioChat);

        if (!coinGains.isEmpty()) return;

        for (Member m : voiceChannel.getMembers()) {
            User u = m.getUser();
            if (u.isBot()) continue;
            coinGains.put(u.getId(), new UserCoinTracker(u, m.getVoiceState().isDeafened(), Radio.getInstance().getOrchestrator().getActivePlaylist().getCoinMultiplier()));
        }
    }

    @Override
    public void onPlaylistChange(Playlist oldPlaylist, Playlist newPlaylist) {
        if (oldPlaylist.getCoinMultiplier() != newPlaylist.getCoinMultiplier()) {

            coinGains.forEach((user, coins) -> coins.setMultiplier(newPlaylist.getCoinMultiplier()));
        }
    }

    @Override
    public void onShutdown() {
        giveAllCoins();
    }

    @Override
    public void onEvent(@Nonnull GenericEvent ev) {
        if (ev instanceof GuildVoiceJoinEvent) { //joined radio vc
            GuildVoiceJoinEvent e = (GuildVoiceJoinEvent) ev;

            if (e.getMember().getUser().isBot()) return;
            if (!voiceChannel.equals(e.getChannelJoined())) return;

            coinGains.put(e.getMember().getUser().getId(), new UserCoinTracker(e.getMember().getUser(), e.getVoiceState().isDeafened(), Radio.getInstance().getOrchestrator().getActivePlaylist().getCoinMultiplier()));

        } else if (ev instanceof GuildVoiceDeafenEvent) { //user deafened
            GuildVoiceDeafenEvent e = (GuildVoiceDeafenEvent) ev;

            if (e.getMember().getUser().isBot()) return;
            if (!voiceChannel.equals(e.getVoiceState().getChannel())) return;

            User user = e.getMember().getUser();

            UserCoinTracker coins = coinGains.get(user.getId());
            if (coins == null) {
                coinGains.put(user.getId(), new UserCoinTracker(user, false, Radio.getInstance().getOrchestrator().getActivePlaylist().getCoinMultiplier()));
            } else {
                coins.setFrozen(e.isDeafened()); //stop tracking coins if they're deafened, resume if undeafened
            }
        } else if (ev instanceof GuildVoiceLeaveEvent) { //left radio vc
            GuildVoiceLeaveEvent e = (GuildVoiceLeaveEvent) ev;

            if (e.getMember().getUser().isBot()) return;
            if (!voiceChannel.equals(e.getChannelLeft())) return;

            giveCoins(e.getMember(), false);
        } else if (ev instanceof GuildVoiceMoveEvent) {
            GuildVoiceMoveEvent e = (GuildVoiceMoveEvent) ev;

            if (e.getMember().getUser().isBot()) return;

            if (voiceChannel.equals(e.getChannelJoined())) {
                coinGains.put(e.getMember().getUser().getId(), new UserCoinTracker(e.getMember().getUser(), e.getVoiceState().isDeafened(), Radio.getInstance().getOrchestrator().getActivePlaylist().getCoinMultiplier()));
            } else if (voiceChannel.equals(e.getChannelLeft())) {
                giveCoins(e.getMember(), false);
            }
        }
    }

    private void giveAllCoins() {
        for (Member m : voiceChannel.getMembers()) {
            UserCoinTracker coins = coinGains.remove(m.getUser().getId());
            if (coins == null) continue; //shouldn't happen

            giveCoins(m, true);
        }

        coinGains.clear();
    }

    private void giveCoins(Member member, boolean silent) {
        User user = member.getUser();
        var id = user.getId();

        UserCoinTracker coins = coinGains.remove(id);
        if (coins == null) return;

        if (coins.getTotalTime() > 18000000) { //5 hours
            Radio.getInstance().getService(AchievementManager.class).rewardAchievement(member.getUser(), Achievement.LISTEN_FOR_5_HOURS_AT_ONCE);
        }

        int amount = coins.getTotal();
        log(ConsoleColor.YELLOW_BACKGROUND_BRIGHT + " COINS " + ConsoleColor.RESET_SPACE + user.getName() + " has earned " + amount + " coins");

        if (amount < 1) return;

        Service.of(CurrencyManager.class).makeTransaction(member, new Transaction(TransactionType.RADIO, amount).addParameter("duration", coins.getTotalTime()));

        if (!silent) {
            var lb = Service.of(UserStatisticsManager.class).getLeaderboard(Statistic.LISTEN_TIME, true, false);
            int pos = 0;

            for (var le : lb) {
                pos++;
                if (le.getUser().equals(id)) {
                    break;
                }
            }

            textChannel.sendMessage(new EmbedBuilder()
                    .setTitle("Earned Degreecoins")
                    .setColor(new Color(110, 230, 140))
                    .setDescription(user.getName() + " has earned " + CurrencyManager.DEGREECOIN_EMOTE + " " + amount + " for listening to the 95 Degrees Radio for " + Formatting.getFormattedMsTimeLabelled(coins.getTotalTime()))
                    .appendDescription("\n\n" + (pos == 1 ? "👑" : "") + "#" + pos + " listener this week!")
                    .setTimestamp(OffsetDateTime.now())
                    .setFooter(user.getName(), user.getAvatarUrl()).build())
                    .queue();

            RPCSocketManager srv = Radio.getInstance().getService(RPCSocketManager.class);
            if (srv != null) {
                srv.sendCoinNotification(user.getId(), amount, coins.getTotalTime());
            }
        }

        if (RadioConfig.config.roles.notificationsOptOutRole == null || member.getRoles().stream().noneMatch(r -> r.getId().equals(RadioConfig.config.roles.notificationsOptOutRole))) {
            user.openPrivateChannel().queue(c -> c.sendMessage(new EmbedBuilder()
                    .setTitle("Earned Degreecoins")
                    .setColor(new Color(110, 230, 140))
                    .setThumbnail("https://cdn.discordapp.com/attachments/476557027431284769/479733204224311296/dc.png")
                    .setDescription("You have earned Đ" + amount + " for listening to the 95 Degrees Radio for " + Formatting.getFormattedMsTimeLabelled(coins.getTotalTime()) + "!")
                    .setTimestamp(OffsetDateTime.now())
                    .setFooter("95 Degrees Radio", user.getJDA().getSelfUser().getAvatarUrl()).build()).queue());
        }
    }
}
