package me.voidinvoid.discordmusic.levelling;

import java.util.List;

public class Level {

    private final int level;
    private final int requiredXp;
    private final int reward;
    private final List<AppliedLevelExtra> extras;

    Level(int level, int requiredXp, int reward, List<AppliedLevelExtra> extras) {

        this.level = level;
        this.requiredXp = requiredXp;
        this.reward = reward;
        this.extras = extras;
    }

    public int getLevel() {
        return level;
    }

    public int getRequiredXp() {
        return requiredXp;
    }

    public int getReward() {
        return reward;
    }

    public List<AppliedLevelExtra> getExtras() {
        return extras;
    }
}
