package me.voidinvoid.discordmusic.levelling;

import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.config.RadioConfig;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import org.bson.Document;

import java.util.List;

import static com.mongodb.client.model.Filters.eq;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
public class AchievementManager {

    public void rewardAchievement(User user, Achievement achievement) {
        DatabaseManager db = Radio.getInstance().getService(DatabaseManager.class);

        Document doc = db.findOrCreateUser(user);

        List<Achievement> achievements = (List<Achievement>) doc.get("achievements"); //todo CHECK
        if (!achievements.contains(achievement)) {
            achievements.add(achievement);
            doc.put("achievements", achievements);

            db.getCollection("users").updateOne(eq("_id", user.getId()), doc);

            TextChannel c = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat);

            c.sendMessage(new EmbedBuilder()
                    .setTitle("Achievement Unlocked")
                    .setDescription(user.getAsMention() + " has unlocked an achievement!")
                    .addField("Achievement", "**" + achievement.getDisplay() + "**\n" + achievement.getDescription(), false)
                    .addField("Reward", "<:degreecoin:431982714212843521> " + achievement.getReward(), false)
                    .build()
            ).queue();
        }
    }
}
