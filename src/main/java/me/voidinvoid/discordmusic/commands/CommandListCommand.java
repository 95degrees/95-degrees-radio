package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Emoji;
import net.dv8tion.jda.api.entities.GuildChannel;

import java.util.List;
import java.util.stream.Collectors;

public class CommandListCommand extends Command {

    private final List<Command> commands;

    CommandListCommand(List<Command> commands) {
        super("commands", "Lists all commands", null, null, "c");
        this.commands = commands;
    }

    @Override
    public void invoke(CommandData data) {
        StringBuilder sb = new StringBuilder(Emoji.RADIO + " | **95 Degrees Radio Commands**\n");

        String cmds = commands.stream().filter(cmd -> (data.isConsole() || cmd.getRank().hasRank(data.getMember()))).map(cmd -> "\t`" + (data.isConsole() ? "" : Command.COMMAND_PREFIX) + cmd.getName() + (cmd.getUsageMessage() == null ? "" : " " + cmd.getUsageMessage()) + "` - " + cmd.getDescription()).collect(Collectors.joining("\n"));

        sb.append(cmds);

        if (cmds.isEmpty()) {
            sb.append(Emoji.CROSS + " You don't have permission to execute any commands");
        }

        data.sendMessage(sb.toString());
    }
}
