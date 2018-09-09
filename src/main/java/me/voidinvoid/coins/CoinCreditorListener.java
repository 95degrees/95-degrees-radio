package me.voidinvoid.coins;

import me.voidinvoid.config.RadioConfig;
import me.voidinvoid.SongOrchestrator;
import me.voidinvoid.utils.ConsoleColor;
import me.voidinvoid.utils.FormattingUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceDeafenEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.hooks.EventListener;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

public class CoinCreditorListener implements EventListener {

    private SongOrchestrator orchestrator;

    private Map<Long, UserCoinTracker> coinGains = new HashMap<>();

    private Map<User, Integer> pendingDatabaseUpdate = new HashMap<>();

    public CoinCreditorListener(SongOrchestrator orchestrator) {

        this.orchestrator = orchestrator;

        for (Member m : orchestrator.getVoiceChannel().getMembers()) {
            User u = m.getUser();
            if (u.isBot()) continue;
            coinGains.put(u.getIdLong(), new UserCoinTracker(u, m.getVoiceState().isDeafened()));
        }
    }

    @Override
    public void onEvent(Event ev) {
        if (ev instanceof GuildVoiceJoinEvent) { //joined radio vc
            GuildVoiceJoinEvent e = (GuildVoiceJoinEvent) ev;

            if (e.getMember().getUser().isBot()) return;
            if (!orchestrator.getVoiceChannel().equals(e.getChannelJoined())) return;

            coinGains.put(e.getMember().getUser().getIdLong(), new UserCoinTracker(e.getMember().getUser(), e.getVoiceState().isDeafened()));

        } else if (ev instanceof GuildVoiceDeafenEvent) { //user deafened
            GuildVoiceDeafenEvent e = (GuildVoiceDeafenEvent) ev;

            if (e.getMember().getUser().isBot()) return;
            if (!orchestrator.getVoiceChannel().equals(e.getVoiceState().getChannel())) return;

            User user = e.getMember().getUser();

            UserCoinTracker coins = coinGains.get(user.getIdLong());
            if (coins == null) {
                coinGains.put(user.getIdLong(), coins = new UserCoinTracker(user, false));
            }

            coins.setFrozen(e.isDeafened()); //stop tracking coins if they're deafened, resume if undeafened
        } else if (ev instanceof GuildVoiceLeaveEvent) { //left radio vc
            GuildVoiceLeaveEvent e = (GuildVoiceLeaveEvent) ev;

            if (e.getMember().getUser().isBot()) return;
            if (!orchestrator.getVoiceChannel().equals(e.getChannelLeft())) return;

            giveCoins(e.getMember(), false);
        } else if (ev instanceof ShutdownEvent) {
            shutdown();
        }
    }

    private void shutdown() {
        for (Member m : orchestrator.getVoiceChannel().getMembers()) {
            UserCoinTracker coins = coinGains.remove(m.getUser().getIdLong());
            if (coins == null) continue; //shouldn't happen

            giveCoins(m, true);
        }

        CoinsServerManager.addCredit(pendingDatabaseUpdate);
        pendingDatabaseUpdate.clear();
    }

    private void giveCoins(Member member, boolean clump) {
        User user = member.getUser();
        long id = user.getIdLong();

        UserCoinTracker coins = coinGains.remove(id);
        if (coins == null) return;

        int amount = coins.getTotal();
        System.out.println(ConsoleColor.YELLOW_BACKGROUND_BRIGHT + " COINS " + ConsoleColor.RESET_SPACE + user.getName() + " has earned " + amount + " coins");

        if (amount < 1) return;

        if (RadioConfig.config.notificationsOptOutRole == null || member.getRoles().stream().noneMatch(r -> r.getId().equals(RadioConfig.config.notificationsOptOutRole))) {
            user.openPrivateChannel().queue(c -> c.sendMessage(new EmbedBuilder()
                    .setTitle("Earned Coins")
                    .setColor(new Color(110, 230, 140))
                    .setThumbnail("https://cdn.discordapp.com/attachments/476557027431284769/479733204224311296/dc.png")
                    .setDescription("You have earned Đ" + amount + " for listening to the 95 Degrees Radio for " + FormattingUtils.getFormattedMsTimeLabelled(coins.getTotalTime()) + "!")
                    .setTimestamp(OffsetDateTime.now())
                    .setFooter("95 Degrees", "https://cdn.discordapp.com/icons/202600401281744896/40d9b8c72e0a288f8f3f5c99ce1691ca.webp").build()).queue());
        }

        if (!clump) {
            orchestrator.getRadioChannel().sendMessage(new EmbedBuilder()
                    .setTitle("Earned Coins")
                    .setColor(new Color(110, 230, 140))
                    .setDescription(user.getName() + " has earned <:degreecoin:431982714212843521> " + amount + " for listening to the 95 Degrees Radio for " + FormattingUtils.getFormattedMsTimeLabelled(coins.getTotalTime()))
                    .setTimestamp(OffsetDateTime.now())
                    .setFooter(user.getName(), user.getAvatarUrl()).build())
                    .queue();
            CoinsServerManager.addCredit(user, amount);
        } else {
            pendingDatabaseUpdate.put(user, pendingDatabaseUpdate.getOrDefault(user, 0) + amount);
        }
    }
}
