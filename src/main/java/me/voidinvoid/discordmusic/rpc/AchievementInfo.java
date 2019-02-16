package me.voidinvoid.discordmusic.rpc;

import me.voidinvoid.discordmusic.levelling.Achievement;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
public class AchievementInfo {

    public String id;
    public String title;
    public String description;
    public int reward;

    public AchievementInfo(Achievement achievement) {

        id = achievement.name();
        title = achievement.getDisplay();
        description = achievement.getDescription();
        reward = achievement.getReward();
    }
}
