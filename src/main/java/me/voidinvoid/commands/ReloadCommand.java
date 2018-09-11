package me.voidinvoid.commands;

import me.voidinvoid.Radio;
import me.voidinvoid.utils.ChannelScope;

public class ReloadCommand extends Command {

    public ReloadCommand() {
        super("reload", "Reloads playlists, tasks and adverts", "[playlists|tasks|adverts]", ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        String[] args = data.getArgs();

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("playlists")) {
                Radio.instance.getOrchestrator().loadPlaylists();
                data.success("Reloaded playlists");

            } else if (args[0].equalsIgnoreCase("tasks")) {
                Radio.instance.startTaskManager();
                data.success("Reloaded tasks");

            } else if (args[0].equalsIgnoreCase("adverts")) {
                if (Radio.instance.getAdvertisementManager() != null) {
                    Radio.instance.getAdvertisementManager().reload();
                    data.success("Reloaded adverts");
                } else {
                    data.error("Adverts are not enabled");
                }
            } else {
                data.error("Unknown parameter. Use `playlists`, `tasks`, or `adverts`");
                return;
            }
        }

        Radio.instance.getOrchestrator().loadPlaylists();
        Radio.instance.startTaskManager();
        if (Radio.instance.getAdvertisementManager() != null) Radio.instance.getAdvertisementManager().reload();

        data.success("Reloaded playlists, tasks and adverts");
    }
}
