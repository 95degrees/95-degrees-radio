package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.utils.ChannelScope;

public class ReloadCommand extends Command {

    ReloadCommand() {
        super("reload", "Reloads playlists. Use !rs to reload other services", null, ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        Radio.getInstance().getOrchestrator().loadPlaylists();

        data.success("Reloaded playlists.\nUse `!rs` to reload other services");
    }
}
