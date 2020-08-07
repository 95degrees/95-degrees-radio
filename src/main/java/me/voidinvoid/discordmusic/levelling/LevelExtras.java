package me.voidinvoid.discordmusic.levelling;

import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.utils.Formatting;

import java.util.function.Function;

public enum LevelExtras {

    MAX_SUGGESTION_LENGTH(RadioConfig.config.orchestration.maxSongLength, "Max suggested song length", o -> Formatting.getFormattedMsTimeLabelled((long) (int) o)),
    MAX_SUGGESTIONS_IN_QUEUE(RadioConfig.config.orchestration.userQueueLimit, "Suggestion queue limit", Object::toString),
    @Deprecated
    SKIP_SONGS_WHEN_ALONE(false, "Skipping songs when alone in the radio", o -> (boolean) o ? "Enabled" : "Disabled");

    private final Object originalValue;
    private final String displayName;
    private final Function<Object, String> formatParameter;

    LevelExtras(Object originalValue, String displayName, Function<Object, String> formatParameter) {

        this.originalValue = originalValue;
        this.displayName = displayName;
        this.formatParameter = formatParameter;
    }

    public Object getOriginalValue() {
        return originalValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String formatParameter(Object param) {
        return formatParameter.apply(param);
    }
}
