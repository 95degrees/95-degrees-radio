package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.utils.ChannelScope;

public class SkipCommand extends Command {

    SkipCommand() {
        super("skip", "Skips the currently playing track", null, ChannelScope.RADIO_AND_DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        if (ChannelScope.DJ_CHAT.check(data.getTextChannel())) {
            Radio.getInstance().getOrchestrator().playNextSong();

            data.success("Skipped to the next track");
        } else {
            data.error("Skipping in the radio text channel is coming soon"); //todo
        }
    }
}
