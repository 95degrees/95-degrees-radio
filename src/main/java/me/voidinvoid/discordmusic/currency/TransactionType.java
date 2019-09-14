package me.voidinvoid.discordmusic.currency;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.utils.Formatting;
import net.dv8tion.jda.api.entities.Member;

public enum TransactionType {
    RADIO("listening to 95 Degrees Radio", new TransactionParameter("duration") {
        @Override
        public String formatValue(Transaction transaction, Object value) {
            return "for " + Formatting.getFormattedMsTime((long) value);
        }
    }),

    RADIO_ACHIEVEMENT("radio achievement reward", new TransactionParameter("achievement") {
        @Override
        public String formatValue(Transaction transaction, Object value) {
            return "for achievement " + value;
        }
    }),

    RADIO_LEVELLING_REWARD("radio levelling reward", new TransactionParameter("level") {
        @Override
        public String formatValue(Transaction transaction, Object value) {
            return "for reaching level " + value;
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
