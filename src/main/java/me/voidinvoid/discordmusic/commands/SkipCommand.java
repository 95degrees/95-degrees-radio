package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.utils.ChannelScope;

public class SkipCommand extends Command {

    SkipCommand() {
        super("skip", "Skips the currently playing track", null, ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        Radio.getInstance().getOrchestrator().playNextSong();

        data.success("Skipped to the next track");
    }
}
