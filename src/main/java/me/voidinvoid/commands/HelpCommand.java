package me.voidinvoid.commands;

import java.util.List;
import java.util.stream.Collectors;

public class HelpCommand extends Command {

    private List<Command> commands;

    public HelpCommand(List<Command> commands) {
        super("radio-commands", "Lists all commands", null, CommandScope.DJ_CHAT, "rc");
        this.commands = commands;
    }

    @Override
    public void invoke(CommandData data) {
        data.getTextChannel().sendMessage("```[Radio commands]\n\n" + commands.stream().filter(c -> !c.equals(this)).map(c -> Command.COMMAND_PREFIX + c.getName() + (c.getUsageMessage() == null ? "" :  " " + c.getUsageMessage()) + " - " + c.getDescription()).collect(Collectors.joining("\n")).replaceAll("`", "") + "```").queue();
    }
}
