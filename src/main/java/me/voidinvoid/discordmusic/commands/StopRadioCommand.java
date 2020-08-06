package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Rank;

public class StopRadioCommand extends Command {

    StopRadioCommand() {
        super("stop", "Shuts down the radio - only do this if something breaks", null, Rank.STAFF);
    }

    @Override
    public void invoke(CommandData data) {
        Radio.getInstance().shutdown(false);
    }
}
