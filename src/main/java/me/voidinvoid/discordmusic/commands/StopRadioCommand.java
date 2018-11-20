package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.utils.ChannelScope;

public class StopRadioCommand extends Command {

    StopRadioCommand() {
        super("stop-radio", "Shuts down the radio - only do this if something breaks", null, ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        Radio.shutdown(false);
    }
}
