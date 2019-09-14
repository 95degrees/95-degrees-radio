package me.voidinvoid.discordmusic.tasks.types;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.SongOrchestrator;
import me.voidinvoid.discordmusic.currency.CurrencyManager;
import me.voidinvoid.discordmusic.currency.Transaction;
import me.voidinvoid.discordmusic.currency.TransactionType;
import me.voidinvoid.discordmusic.levelling.Achievement;
import me.voidinvoid.discordmusic.levelling.AchievementManager;
import me.voidinvoid.discordmusic.stats.Statistic;
import me.voidinvoid.discordmusic.stats.UserStatisticsManager;
import me.voidinvoid.discordmusic.tasks.ParameterList;
import me.voidinvoid.discordmusic.tasks.RadioTaskExecutor;
import me.voidinvoid.discordmusic.utils.Colors;
import me.voidinvoid.discordmusic.utils.Service;
import net.dv8tion.jda.api.EmbedBuilder;

import java.time.OffsetDateTime;

public class LeaderboardRewardTask extends RadioTaskExecutor {

    @Override
    public void runTask(SongOrchestrator orch, ParameterList params) {

        var stat = Statistic.valueOf(params.get("type", String.class));
        var reward = params.get("reward", Integer.class);

        var lb = Service.of(UserStatisticsManager.class).getLeaderboard(stat, true, false);

        if (lb.isEmpty()) return;

        for (var winner : lb) {
            if (winner.getValue() <= 0) return; //no winner

            var mb = Radio.getInstance().getGuild().getMemberById(winner.getUser());

            if (mb == null) continue;
            Service.of(CurrencyManager.class).makeTransaction(mb, new Transaction(TransactionType.RADIO, reward));

            var am = Service.of(AchievementManager.class);
            am.rewardAchievement(mb.getUser(), Achievement.TOP_WEEKLY_LISTENER);

            mb.getUser().openPrivateChannel().queue(c -> c.sendMessage(new EmbedBuilder()
                    .setTitle("Most Active Listener")
                    .setDescription("Well done! You were the most active listener of the 95 Degrees Radio this week! Thanks for listening!")
                    .addField("Reward", "Đ" + reward, false)
                    .setFooter("95 Degrees Radio", mb.getJDA().getSelfUser().getAvatarUrl())
                    .setTimestamp(OffsetDateTime.now())
                    .setColor(Colors.ACCENT_LEVEL_UP)
                    .build()).queue());

            return;
        }
    }
}
