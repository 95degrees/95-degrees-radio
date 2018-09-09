package me.voidinvoid.commands;

public abstract class Command {

    public static final String COMMAND_PREFIX = "!";

    private final String name;
    private final String description;
    private String usageMessage;
    private CommandScope scope;
    private final String[] aliases;

    public Command(String name, String description, String usageMessage, CommandScope scope, String... aliases) {

        this.name = name;
        this.description = description;
        this.usageMessage = usageMessage;
        this.scope = scope;
        this.aliases = aliases;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUsageMessage() {
        return usageMessage;
    }

    public CommandScope getScope() {
        return scope;
    }

    public String[] getAliases() {
        return aliases;
    }

    public abstract void invoke(CommandData data);
}
