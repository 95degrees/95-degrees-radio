package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.utils.ChannelScope;

public class RestartRadioCommand extends Command {

    RestartRadioCommand() {
        super("restart-radio", "Restarts the radio", null, ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        Radio.getInstance().shutdown(true);
    }
}
