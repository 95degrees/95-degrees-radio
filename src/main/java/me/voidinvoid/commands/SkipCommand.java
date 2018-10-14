package me.voidinvoid.commands;

import me.voidinvoid.Radio;
import me.voidinvoid.utils.ChannelScope;

public class SkipCommand extends Command {

    SkipCommand() {
        super("skip", "Skips the currently playing track", null, ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        Radio.instance.getOrchestrator().playNextSong();

        data.success("Skipped to the next track");
    }
}
