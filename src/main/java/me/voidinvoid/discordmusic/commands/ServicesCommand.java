package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Rank;

import java.util.Optional;
import java.util.stream.Collectors;

public class ServicesCommand extends Command {

    ServicesCommand() {
        super("services", "List or reload radio services", "<list|reload ...>", Rank.STAFF, "s");
    }

    @Override
    public void invoke(CommandData data) {
        String[] args = data.getArgs();
        if (args.length < 1) {
            data.error("`list`, `reload` or `stop` required");

        } else if (args[0].equals("list")) {
            data.success("Services: \n" + Radio.getInstance().getServices().stream().map(r -> r.getClass().getSimpleName()).collect(Collectors.joining("\n")));

        } else if (args[0].equals("reload")) {
            if (args.length < 2) {
                data.error("Service name required. List using `!rs list`");
                return;
            }

            Optional<RadioService> rs = Radio.getInstance().getServices().stream().filter(r -> r.getClass().getSimpleName().equalsIgnoreCase(args[1])).findAny();

            if (rs.isEmpty()) {
                data.error("Invalid service name. List using `!rs list`");
            } else {
                rs.get().onShutdown();
                rs.get().onLoad();
                data.success("Reloaded " + rs.get().getClass().getSimpleName());
            }

        } else if (args[0].equalsIgnoreCase("stop")) {
            if (args.length < 2) {
                data.error("Service name required. List using `!rs list`");
                return;
            }

            Optional<RadioService> rs = Radio.getInstance().getServices().stream().filter(r -> r.getClass().getSimpleName().equalsIgnoreCase(args[1])).findAny();

            if (rs.isEmpty()) {
                data.error("Invalid service name. List using `!rs list`");
            } else {
                rs.get().onShutdown();
                data.success("Shutdown " + rs.get().getClass().getSimpleName() + ". This may cause some stability issues");
            }

        } else {
            data.error("`list`, `reload` or `stop` required");
        }
    }
}
