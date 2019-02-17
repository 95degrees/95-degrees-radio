package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.utils.ChannelScope;

abstract class Command {

    public static final String COMMAND_PREFIX = "!";

    private final String name;
    private final String description;
    private final String usageMessage;
    private final ChannelScope scope;
    private final boolean allowConsole;
    private final String[] aliases;

    Command(String name, String description, String usageMessage, ChannelScope scope, String... aliases) {

        this(name, description, usageMessage, scope, true, aliases);
    }

    Command(String name, String description, String usageMessage, ChannelScope scope, boolean allowConsole, String... aliases) {

        this.name = name;
        this.description = description;
        this.usageMessage = usageMessage;
        this.scope = scope;
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

    public ChannelScope getScope() {
        return scope;
    }

    public boolean isAllowConsole() {
        return allowConsole;
    }

    public String[] getAliases() {
        return aliases;
    }

    public abstract void invoke(CommandData data);
}
