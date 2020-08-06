package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.utils.Rank;

abstract class Command {

    public static final String COMMAND_PREFIX = "r!";

    private final String name;
    private final String description;
    private final String usageMessage;
    private final Rank rank;
    private final boolean allowConsole;
    private final String[] aliases;

    Command(String name, String description, String usageMessage, Rank rank, String... aliases) {

        this(name, description, usageMessage, rank, true, aliases);
    }

    Command(String name, String description, String usageMessage, Rank rank, boolean allowConsole, String... aliases) {

        this.name = name;
        this.description = description;
        this.usageMessage = usageMessage;
        this.rank = rank == null ? Rank.NORMAL : rank;
        this.allowConsole = allowConsole;
        this.aliases = aliases;
    }

    public String getName() {
        return name;
    }

    String getDescription() {
        return description;
    }

    public String getUsageMessage() {
        return usageMessage;
    }

    public Rank getRank() {
        return rank;
    }

    public boolean isAllowConsole() {
        return allowConsole;
    }

    public String[] getAliases() {
        return aliases;
    }

    public abstract void invoke(CommandData data);
}
