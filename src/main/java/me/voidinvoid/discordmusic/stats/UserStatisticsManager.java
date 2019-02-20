package me.voidinvoid.discordmusic.stats;

import com.mongodb.client.MongoCollection;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.events.SongEventListener;
import me.voidinvoid.discordmusic.songs.NetworkSong;
import me.voidinvoid.discordmusic.utils.Service;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import org.bson.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
public class UserStatisticsManager implements RadioService, SongEventListener {

    private MongoCollection<Document> users;

    @Override
    public void onLoad() {
        users = Service.of(DatabaseManager.class).getCollection("users");
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

    public List<LeaderboardEntry> getLeaderboard(Statistic stat, boolean weekly) { //todo make sure this isnt called too often, may be fairly intensive
        var day = 7 - LocalDateTime.now().getDayOfWeek().getValue() - 1; //TODO check 7
        return users.find().into(new ArrayList<>()).stream().map(d -> new LeaderboardEntry(d.getString("_id"), weekly ? getStatistic(d, stat, day, 7) : getTotal(d, stat))).sorted().collect(Collectors.toList());
    }

    private String getDate(int dayOffset) {
        return LocalDateTime.now().minusDays(dayOffset).format(DateTimeFormatter.ISO_DATE);
    }

    public void addStatistic(User user, Statistic stat, int amount) {
        users.updateOne(eq(user.getId()), new Document("$inc", new Document("stats." + getDate(0) + "." + stat.name().toLowerCase(), amount)));
    }

    @Override
    public void onNetworkSongQueued(NetworkSong song, AudioTrack track, Member member, int queuePosition) {
        addStatistic(member.getUser(), Statistic.SONGS_SUGGESTED, 1);
    }
}
