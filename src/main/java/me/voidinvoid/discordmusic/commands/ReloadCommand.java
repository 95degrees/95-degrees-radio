package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.advertisements.AdvertisementManager;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.tasks.TaskManager;
import me.voidinvoid.discordmusic.utils.ChannelScope;

public class ReloadCommand extends Command {

    ReloadCommand() {
        super("reload", "Reloads playlists, tasks and adverts", "[playlists|tasks|adverts]", ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        String[] args = data.getArgs(); //TODO this should dynamically change, classes can inherit 'ReloadableRadioService' and these can auto be populated into this cmd

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("playlists")) {
                Radio.getInstance().getOrchestrator().loadPlaylists();
                data.success("Reloaded playlists");

            } else if (args[0].equalsIgnoreCase("tasks")) {
                Radio.getInstance().getService(TaskManager.class).reload();
                data.success("Reloaded tasks");

            } else if (args[0].equalsIgnoreCase("adverts") || args[0].equalsIgnoreCase("ads")) {
                AdvertisementManager adman = Radio.getInstance().getService(AdvertisementManager.class);
                if (adman != null) {
                    adman.reload();
                    data.success("Reloaded adverts");
                } else {
                    data.error("The advertisement service is currently disabled");
                }

            } else if (args[0].equalsIgnoreCase("config")) {
                //Radio.getInstance().reloadConfig();
            } else {
                data.error("Unknown parameter. Use `playlists`, `tasks`, `adverts` or `config`");
            }

            return;
        }

        Radio.getInstance().getOrchestrator().loadPlaylists();
        Radio.getInstance().getService(TaskManager.class).reload();
        AdvertisementManager adman = Radio.getInstance().getService(AdvertisementManager.class);
        if (adman != null) {
            adman.reload();
        }

        data.success("Reloaded playlists, tasks and adverts");
    }
}
