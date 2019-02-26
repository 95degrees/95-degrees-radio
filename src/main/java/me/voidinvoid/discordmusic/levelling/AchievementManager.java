package me.voidinvoid.discordmusic.levelling;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.coins.CoinsServerManager;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.currency.CurrencyManager;
import me.voidinvoid.discordmusic.events.SongEventListener;
import me.voidinvoid.discordmusic.rpc.RPCSocketManager;
import me.voidinvoid.discordmusic.songs.NetworkSong;
import me.voidinvoid.discordmusic.utils.Colors;
import me.voidinvoid.discordmusic.utils.Service;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import org.bson.Document;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
public class AchievementManager implements RadioService, SongEventListener {

    @SuppressWarnings("unchecked")
    public void rewardAchievement(User user, Achievement achievement) {
        DatabaseManager db = Radio.getInstance().getService(DatabaseManager.class);
        Document doc = db.findOrCreateUser(user);

        List<String> achievements = (List<String>) doc.get("achievements");
        if (!achievements.contains(achievement.name())) {
            db.getCollection("users").updateOne(eq(user.getId()), new Document("$addToSet", new Document("achievements", achievement.name())));

            TextChannel c = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat);

            c.sendMessage(new MessageBuilder("🎉 " + user.getAsMention()).setEmbed(new EmbedBuilder()
                    .setTitle("Achievement Unlocked")
                    .setColor(Colors.ACCENT_ACHIEVEMENT)
                    .setThumbnail(RadioConfig.config.images.achievementLogo)
                    .setDescription(user.getAsMention() + " has unlocked an achievement!")
                    .addField(achievement.getDisplay(), achievement.getDescription(), false)
                    .addField("Reward", CurrencyManager.DEGREECOIN_EMOTE + " " + achievement.getReward(), false)
                    .build()).build()).queue();

            Service.of(CoinsServerManager.class).addCredit(user, achievement.getReward());

            var rpc = Radio.getInstance().getService(RPCSocketManager.class);
            if (rpc != null) {
                rpc.notifyAchievement(user, achievement);
            }
        }
    }

    @Override
    public void onNetworkSongQueued(NetworkSong song, AudioTrack track, Member member, int queuePosition) {
        if (track.getIdentifier().endsWith("dQw4w9WgXcQ") && member != null) {
            rewardAchievement(member.getUser(), Achievement.RICKROLL);
        }
    }
}
