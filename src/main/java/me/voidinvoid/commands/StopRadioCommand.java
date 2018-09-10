package me.voidinvoid.commands;

import me.voidinvoid.DiscordRadio;

public class StopRadioCommand extends Command {

    public StopRadioCommand() {
        super("stop-radio", "Shuts down the radio - only do this if something breaks", null, CommandScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        DiscordRadio.shutdown(false);
    }
}
