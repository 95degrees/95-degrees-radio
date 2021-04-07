package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Colors;
import me.voidinvoid.discordmusic.utils.Emoji;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildChannel;

import java.util.List;
import java.util.stream.Collectors;

public class CommandListCommand extends Command {

    private final List<Command> commands;

    CommandListCommand(List<Command> commands) {
        super("commands", "Lists all commands", null, null, "c", "help", "?");
        this.commands = commands;
    }

    @Override
    public void invoke(CommandData data) {
        var embed = new EmbedBuilder().setTitle(Emoji.RADIO + " 95 Degrees Radio Commands").setColor(Colors.ACCENT_MAIN);

        String cmds = commands.stream().filter(cmd -> (data.isConsole() || cmd.getRank().hasRank(data.getMember()))).map(cmd -> "\t`" + (data.isConsole() ? "" : Command.COMMAND_PREFIX) + cmd.getName() + (cmd.getUsageMessage() == null ? "" : " " + cmd.getUsageMessage()) + "` - " + cmd.getDescription()).collect(Collectors.joining("\n"));

        if (cmds.isEmpty()) {
            embed.setDescription(Emoji.CROSS + " You don't have permission to execute any commands");
        } else {
            embed.setDescription(cmds);
        }

        data.embed(embed.build());
    }
}
