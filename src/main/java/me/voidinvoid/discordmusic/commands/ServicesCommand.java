package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.utils.ChannelScope;

import java.util.Optional;
import java.util.stream.Collectors;

public class ServicesCommand extends Command {

    ServicesCommand() {
        super("radio-services", "List or reload radio services", "<list|reload ...>", ChannelScope.DJ_CHAT, "rs");
    }

    @Override
    public void invoke(CommandData data) {
        String[] args = data.getArgs();
        if (args.length < 1) {
            data.error("`list` or `reload` required");

        } else if (args[0].equals("list")) {
            data.success("Active services: \n" + Radio.getInstance().getServices().stream().map(r -> r.getClass().getTypeName()).collect(Collectors.joining("\n")));

        } else if (args[0].equals("reload")) {
            if (args.length < 2) {
                data.error("Service name required. List using `!rs list`");
                return;
            }

            Optional<RadioService> rs = Radio.getInstance().getServices().stream().filter(r -> r.getClass().getTypeName().equalsIgnoreCase(args[1])).findAny();

            if (!rs.isPresent()) {
                data.error("Invalid service name. List using `!rs list`");
            } else {
                rs.get().onLoad();
                data.success("Reloaded " + rs.get().getClass().getTypeName());
            }

        } else {
            data.error("`list` or `reload` required");
        }
    }
}
