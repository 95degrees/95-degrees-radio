package me.voidinvoid.discordmusic.stats;

import com.mongodb.client.MongoCollection;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.events.SongEventListener;
import me.voidinvoid.discordmusic.songs.NetworkSong;
import me.voidinvoid.discordmusic.utils.Formatting;
import me.voidinvoid.discordmusic.utils.Service;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import org.bson.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
public class UserStatisticsManager implements RadioService, SongEventListener {

    private MongoCollection<Document> users;
    private TextChannel leaderboardChannel;

    private Map<Statistic, List<LeaderboardEntry>> cachedLeaderboards = new HashMap<>();

    @Override
    public void onLoad() {
        users = Service.of(DatabaseManager.class).getCollection("users");
        leaderboardChannel = Radio.getInstance().getJda().getTextChannelById("547854805037482034"); //TODO
    }

    private void updateLeaderboard(Statistic stat) {
        var lb = cachedLeaderboards.get(stat);

        if (lb == null || !stat.isCreateLeaderboard()) return;

        StringBuilder msg = new StringBuilder("```[Radio ").append(stat.getDisplayName()).append( " Leaderboard]\n");
        msg.append("📅 Weekly\n\n");

        int ix = 0;
        for (var record : lb) {
            var user = Radio.getInstance().getGuild().getMemberById(record.getUser());
            if (user == null) continue;

            ix++;

            msg.append("#").append(ix).append(" - ").append(Formatting.padString(user.getUser().getAsTag(), 30)).append(stat.format(record.getValue())).append("\n");
        }

        leaderboardChannel.sendMessage(msg.append("```").toString()).queue(); //todo
    }

    public int getStatisticFromWeekStart(User user, Statistic stat) {
        var day = LocalDateTime.now().getDayOfWeek().getValue() - 1; //if today is monday, subtract 0 days. if today is sunday, subtract 6 days

        return getStatistic(users.find(eq(user.getId())).first(), stat, day, 7);
    }

    public int getWeeklyStatistic(Document doc, Statistic stat) {
        return getStatistic(doc, stat, 0, 7);
    }

    public int getStatistic(Document doc, Statistic stat, int startOffset, int numOfDays) {
        log("a");
        if (doc == null || !doc.containsKey("stats")) return 0;
        log("b");
        var h = doc.get("stats", Document.class);
        log("c");

        int amount = 0;
        var s = stat.name().toLowerCase();
        log(s);
        log(numOfDays);

        for (int i = 0; i < numOfDays; i++) {
            var day = getDate(i + startOffset); //gets up to numOfDays days in the past with offset
            log(day);
            if (h.containsKey(day)) {
                log("contains");
                var d = h.get(day, Document.class);
                log(d.getInteger(s, -1000));

                amount += d.getInteger(s, 0);
            }
        }

        return amount;
    }

    public int getTotal(User user, Statistic stat) {
        return getTotal(users.find(eq(user.getId())).first(), stat);
    }

    public int getTotal(Document doc, Statistic stat) {
        if (doc == null || !doc.containsKey("stats")) return 0;
        var h = doc.get("stats", Document.class);

        int amount = 0;
        var s = stat.name().toLowerCase();

        for (var obj : h.values()) {
            if (obj instanceof Document) { //should always be
                var d = (Document) obj;

                if (d.containsKey(s)) amount += d.getInteger(s, 0);
            }
        }

        return amount;
    }

    public List<LeaderboardEntry> getLeaderboard(Statistic stat, boolean weekly, boolean force) {
        if (!weekly && !force && cachedLeaderboards.containsKey(stat)) return cachedLeaderboards.get(stat);

        var day = LocalDateTime.now().getDayOfWeek().getValue(); //TODO check 7
        var res = users.find().into(new ArrayList<>()).stream().map(d -> new LeaderboardEntry(d.getString("_id"), weekly ? getStatistic(d, stat, 0, day) : getTotal(d, stat))).sorted().collect(Collectors.toList());

        cachedLeaderboards.put(stat, res);
        updateLeaderboard(stat);
        return res;
    }

    private String getDate(int dayOffset) {
        return LocalDateTime.now().minusDays(dayOffset).format(DateTimeFormatter.ISO_DATE);
    }

    public void addStatistic(User user, Statistic stat, int amount) {
        users.updateOne(eq(user.getId()), new Document("$inc", new Document("stats." + getDate(0) + "." + stat.name().toLowerCase(), amount)));

        cachedLeaderboards.put(stat, getLeaderboard(stat, true, true));
    }

    @Override
    public void onNetworkSongQueued(NetworkSong song, AudioTrack track, Member member, int queuePosition) {
        addStatistic(member.getUser(), Statistic.SONGS_SUGGESTED, 1);
    }
}
