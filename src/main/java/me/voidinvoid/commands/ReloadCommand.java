package me.voidinvoid.commands;

import me.voidinvoid.Radio;
import me.voidinvoid.utils.ChannelScope;

public class ReloadCommand extends Command {

    public ReloadCommand() {
        super("reload", "Reloads playlists and tasks", null, ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        Radio.instance.startTaskManager();
        Radio.instance.getOrchestrator().loadPlaylists();
        if (Radio.instance.getAdvertisementManager() != null) Radio.instance.getAdvertisementManager().reload();

        data.getTextChannel().sendMessage("Reloaded playlists, tasks and adverts").queue();
    }
}
