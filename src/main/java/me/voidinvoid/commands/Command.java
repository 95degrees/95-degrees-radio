package me.voidinvoid.commands;

import me.voidinvoid.utils.ChannelScope;

abstract class Command {

    public static final String COMMAND_PREFIX = "!";

    private final String name;
    private final String description;
    private final String usageMessage;
    private final ChannelScope scope;
    private final String[] aliases;

    Command(String name, String description, String usageMessage, ChannelScope scope, String... aliases) {

        this.name = name;
        this.description = description;
        this.usageMessage = usageMessage;
        this.scope = scope;
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

    public String[] getAliases() {
        return aliases;
    }

    public abstract void invoke(CommandData data);
}
