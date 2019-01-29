package me.voidinvoid.discordmusic.levelling;

public class AppliedLevelExtra {

    private final LevelExtras extra;
    private final Object value;

    public AppliedLevelExtra(LevelExtras extra, Object value) {

        this.extra = extra;
        this.value = value;
    }

    public LevelExtras getExtra() {
        return extra;
    }

    public Object getValue() {
        return value;
    }
}
