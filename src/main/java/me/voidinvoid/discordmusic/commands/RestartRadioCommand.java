package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Rank;

public class RestartRadioCommand extends Command {

    RestartRadioCommand() {
        super("restart", "Restarts the radio", null, Rank.STAFF);
    }

    @Override
    public void invoke(CommandData data) {
        Radio.getInstance().shutdown(true);
    }
}
