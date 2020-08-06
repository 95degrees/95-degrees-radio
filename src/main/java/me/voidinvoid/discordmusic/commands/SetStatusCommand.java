package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Rank;
import net.dv8tion.jda.api.entities.Activity;

public class SetStatusCommand extends Command {

    SetStatusCommand() {
        super("status", "Sets the status of the Discord bot", "<status ...>", Rank.DJ);
    }

    @Override
    public void invoke(CommandData data) {
        if (data.getArgsString().isEmpty()) {
            data.error("Discord status required");
            return;
        }

        Radio.getInstance().getJda().getPresence().setActivity(Activity.playing(data.getArgsString()));
        data.success("Discord status has been changed.\nℹ This may be overridden by playlists or songs when they are played");
    }
}
