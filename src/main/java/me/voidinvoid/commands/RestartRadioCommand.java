package me.voidinvoid.commands;

import me.voidinvoid.Radio;
import me.voidinvoid.utils.ChannelScope;

public class RestartRadioCommand extends Command {

    public RestartRadioCommand() {
        super("restart-radio", "Restarts the radio", null, ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        Radio.shutdown(true);
    }
}
