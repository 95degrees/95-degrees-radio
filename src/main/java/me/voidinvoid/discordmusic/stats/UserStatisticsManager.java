package me.voidinvoid.discordmusic.stats;

import com.mongodb.client.MongoCollection;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.events.SongEventListener;
import me.voidinvoid.discordmusic.songs.NetworkSong;
import me.voidinvoid.discordmusic.utils.Formatting;
import me.voidinvoid.discordmusic.utils.Service;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
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
    private Message leaderboardMessage;

    private Map<Statistic, List<LeaderboardEntry>> cachedLeaderboards = new HashMap<>();

    @Override
    public void onLoad() {
        var db = Service.of(DatabaseManager.class);
        users = db.getCollection("users");
        leaderboardChannel = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.leaderboards); //TODO

        var msgId = db.getInternalDocument().getString("leaderboardMessageId");
        if (msgId != null) {
            leaderboardMessage = leaderboardChannel.getMessageById(msgId).complete();
        }
    }

    private void updateLeaderboard(Statistic stat) {
        var lb = cachedLeaderboards.get(stat);

        if (lb == null || !stat.isCreateLeaderboard()) return;

        StringBuilder msg = new StringBuilder("```RADIO ").append(stat.getDisplayName().toUpperCase()).append( " LEADERBOARD\n");
        msg.append("📅 Resets Weekly\n\n");

        int ix = 0;
        for (var record : lb) {
            var user = Radio.getInstance().getGuild().getMemberById(record.getUser());
            if (user == null) continue;

            ix++;

            if (ix < 10) msg.append("0");
            msg.append(ix).append(". ").append(Formatting.padString(user.getUser().getAsTag(), 50)).append(stat.format(record.getValue())).append("\n");

            if (ix >= 10) break;
        }

        var code = msg.append("```").toString();

        if (leaderboardMessage == null) {
            leaderboardChannel.sendMessage(code).queue(m -> {
                leaderboardMessage = m;
                Service.of(DatabaseManager.class).updateInternalDocument(new Document("$set", new Document("leaderboardMessageId", m.getId())));
            });
        } else {
            leaderboardMessage.editMessage(code).queue();
        }
    }

    public int getStatisticFromWeekStart(User user, Statistic stat) {
        var day = LocalDateTime.now().getDayOfWeek().getValue() - 1; //if today is monday, subtract 0 days. if today is sunday, subtract 6 days

        return getStatistic(users.find(eq(user.getId())).first(), stat, day, 7);
    }

    public int getWeeklyStatistic(Document doc, Statistic stat) {
        return getStatistic(doc, stat, 0, 7);
    }

    public int getStatistic(Document doc, Statistic stat, int startOffset, int numOfDays) {
        if (doc == null || !doc.containsKey("stats")) return 0;
        var h = doc.get("stats", Document.class);

        int amount = 0;
        var s = stat.name().toLowerCase();

        for (int i = 0; i < numOfDays; i++) {
            var day = getDate(i + startOffset); //gets up to numOfDays days in the past with offset
            if (h.containsKey(day)) {
                var d = h.get(day, Document.class);

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
