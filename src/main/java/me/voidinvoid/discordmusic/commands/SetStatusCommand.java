package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import net.dv8tion.jda.core.entities.Game;

public class SetStatusCommand extends Command {

    SetStatusCommand() {
        super("radio-status", "Sets the status of the Discord bot", "<status ...>", ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        if (data.getArgsString().isEmpty()) {
            data.error("Discord status required");
            return;
        }

        Radio.getInstance().getJda().getPresence().setGame(Game.playing(data.getArgsString()));
        data.success("Discord status has been changed.\nℹ This may be overridden by playlists or songs when they are played");
    }
}
