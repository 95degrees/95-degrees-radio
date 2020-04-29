package me.voidinvoid.discordmusic.economy;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
public abstract class TransactionParameter {

    private final String internal;

    public TransactionParameter(String internal) {

        this.internal = internal;
    }

    public abstract String formatValue(Transaction transaction, Object value);

    public String getInternal() {
        return internal;
    }
}
