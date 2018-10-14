package me.voidinvoid.commands;

import me.voidinvoid.utils.ChannelScope;

import java.util.List;
import java.util.stream.Collectors;

public class HelpCommand extends Command {

    private final List<Command> commands;

    HelpCommand(List<Command> commands) {
        super("radio-commands", "Lists all commands", null, ChannelScope.DJ_CHAT, "rc");
        this.commands = commands;
    }

    @Override
    public void invoke(CommandData data) {
        data.code("[Radio commands]\n\n" + commands.stream().filter(c -> !c.equals(this)).map(c -> (data.isConsole() ? "" : Command.COMMAND_PREFIX) + c.getName() + (c.getUsageMessage() == null ? "" : " " + c.getUsageMessage()) + " - " + c.getDescription()).collect(Collectors.joining("\n")).replaceAll("`", ""));
    }
}
