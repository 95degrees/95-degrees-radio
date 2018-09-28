package me.voidinvoid.commands;

import me.voidinvoid.Radio;
import me.voidinvoid.config.RadioConfig;
import me.voidinvoid.utils.ChannelScope;
import net.dv8tion.jda.core.entities.TextChannel;

public class KaraokeCommand extends Command {

    KaraokeCommand() {
        super("karaoke", "Starts or stops karaoke mode (shows song lyrics)", "[lyrics-channel#]", ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        String[] args = data.getArgs();

        TextChannel channel;

        if (args.length == 0 && RadioConfig.config.channels.lyricsChat == null) {
            data.error("No lyrics channel ID supplied and no default lyrics channel is configured");
            return;
        } else if (args.length == 0) {
            channel = Radio.instance.getJda().getTextChannelById(RadioConfig.config.channels.lyricsChat);
        } else {
            channel = Radio.instance.getJda().getTextChannelById(args[0]);

            if (channel == null) {
                data.error("Specified lyrics channel ID is invalid");
                return;
            }
        }

        Radio.instance.getKaraokeManager().setKaraokeMode(!Radio.instance.getKaraokeManager().isKaraokeMode(), channel);
    }
}
