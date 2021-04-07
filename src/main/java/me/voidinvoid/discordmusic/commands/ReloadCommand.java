package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Rank;

public class ReloadCommand extends Command {

    ReloadCommand() {
        super("reload", "Reloads playlists. Use r!rs to reload other services", null, Rank.DJ);
    }

    @Override
    public void invoke(CommandData data) {
        Radio.getInstance().getOrchestrator().loadPlaylists();

        data.success("Reloaded playlists.\nUse `r!rs` to reload other services");
    }
}
