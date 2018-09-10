package me.voidinvoid.commands;

import me.voidinvoid.DiscordRadio;
import me.voidinvoid.config.RadioConfig;
import me.voidinvoid.tasks.RadioTaskComposition;
import me.voidinvoid.tasks.TaskManager;
import net.dv8tion.jda.core.entities.TextChannel;

public class KaraokeCommand extends Command {

    public KaraokeCommand() {
        super("karaoke", "Starts or stops karaoke mode (shows song lyrics)", "[lyrics-channel#]", CommandScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        String[] args = data.getArgs();

        TextChannel channel;

        if (args.length == 0 && RadioConfig.config.channels.lyricsChat == null) {
            data.error("No lyrics channel ID supplied and no default lyrics channel is configured");
            return;
        } else if (args.length == 0) {
            channel = DiscordRadio.instance.getJda().getTextChannelById(RadioConfig.config.channels.lyricsChat);
        } else {
            channel = DiscordRadio.instance.getJda().getTextChannelById(args[0]);

            if (channel == null) {
                data.error("Specified lyrics channel ID is invalid");
                return;
            }
        }

        DiscordRadio.instance.getKaraokeManager().setKaraokeMode(!DiscordRadio.instance.getKaraokeManager().isKaraokeMode(), channel);
    }
}
