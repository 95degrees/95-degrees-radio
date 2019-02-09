package me.voidinvoid.discordmusic.currency;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.utils.FormattingUtils;
import net.dv8tion.jda.core.entities.Member;

public enum TransactionType {
    RADIO("listening to 95 Degrees Radio", new TransactionParameter("duration") {
        @Override
        public String formatValue(Transaction transaction, Object value) {
            return "for " + FormattingUtils.getFormattedMsTime((long) value);
        }
    });

    private final String displayName;
    private final TransactionParameter[] parameters;

    TransactionType(String displayName, TransactionParameter... parameters) {
        this.displayName = displayName;
        this.parameters = parameters;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TransactionParameter[] getParameters() {
        return parameters;
    }

    private static String nameFromId(String id) {
        try {
            Member m = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat).getGuild().getMemberById(id);
            if (m == null) return id;
            return m.getUser().getAsTag() + " (" + id + ")";
        } catch (Exception ignored) {
            return id;
        }
    }
}
