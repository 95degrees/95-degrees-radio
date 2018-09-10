package me.voidinvoid.commands;

import me.voidinvoid.DiscordRadio;

public class RestartRadioCommand extends Command {

    public RestartRadioCommand() {
        super("restart-radio", "Restarts the radio", null, CommandScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        DiscordRadio.shutdown(true);
    }
}
