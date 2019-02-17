package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.utils.ChannelScope;

import java.util.List;
import java.util.stream.Collectors;

public class HelpCommand extends Command {

    private final List<Command> commands;

    HelpCommand(List<Command> commands) {
        super("radio-commands", "Lists all commands", null, ChannelScope.RADIO_AND_DJ_CHAT, "rc");
        this.commands = commands;
    }

    @Override
    public void invoke(CommandData data) {
        data.code("[Radio Commands]\n\n" + commands.stream().filter(c -> !c.equals(this) && (data.getTextChannel() == null || c.getScope().check(data.getTextChannel()))).map(c -> (data.isConsole() ? "" : Command.COMMAND_PREFIX) + c.getName() + (c.getUsageMessage() == null ? "" : " " + c.getUsageMessage()) + " - " + c.getDescription()).collect(Collectors.joining("\n")).replaceAll("`", ""));
    }
}
