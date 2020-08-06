package me.voidinvoid.discordmusic.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Formatting;
import me.voidinvoid.discordmusic.utils.Rank;

public class SeekCommand extends Command {

    SeekCommand() {
        super("seek", "Seeks the current song to a specified position (in seconds)", "<seconds>", Rank.DJ);
    }

    @Override
    public void invoke(CommandData data) {
        String[] args = data.getArgs();

        AudioTrack track = Radio.getInstance().getOrchestrator().getPlayer().getPlayingTrack();

        if (args.length < 1) {
            data.error("Time to seek to required (in secs)");
            return;
        }

        if (track == null) {
            data.error("No track is currently playing");
            return;
        }

        if (!track.isSeekable()) {
            data.error("This track isn't seekable");
            return;
        }

        long time;
        try {
            time = Integer.valueOf(args[0]) * 1000;
        } catch (Exception ignored) {
            data.error("Invalid number of seconds");
            return;
        }

        if (time < 0) {
            data.error("Invalid number of seconds");
            return;
        }

        Radio.getInstance().getOrchestrator().seekTrack(time);
        data.success("Seeked track to " + Formatting.getFormattedMsTime(time));
    }
}
