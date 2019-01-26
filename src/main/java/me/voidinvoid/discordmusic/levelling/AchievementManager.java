package me.voidinvoid.discordmusic.levelling;

import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.utils.Colors;
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

        List<String> achievements = (List<String>) doc.get("achievements"); //todo CHECK
        if (!achievements.contains(achievement.name())) {

            db.getCollection("users").updateOne(eq("_id", user.getId()), new Document("$addToSet", new Document("achievements", achievement.name())));

            TextChannel c = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat);

            c.sendMessage(new EmbedBuilder()
                    .setTitle("Achievement Unlocked")
                    .setColor(Colors.ACCENT_ACHIEVEMENT)
                    .setThumbnail("https://cdn.discordapp.com/attachments/505174503752728597/537703976032796712/todo.png")
                    .setDescription(user.getAsMention() + " has unlocked an achievement!")
                    .addField(achievement.getDisplay(), achievement.getDescription(), false)
                    .addField("Reward", "<:degreecoin:431982714212843521> " + achievement.getReward(), false)
                    .build()
            ).queue();
        }
    }
}
