package me.voidinvoid.commands;

import me.voidinvoid.DiscordRadio;

public class ReloadCommand extends Command {

    public ReloadCommand() {
        super("reload", "Reloads playlists and tasks", null, CommandScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        DiscordRadio.instance.startTaskManager();
        DiscordRadio.instance.getOrchestrator().loadPlaylists();

        data.getTextChannel().sendMessage("Reloaded playlists and tasks").queue();
    }
}
