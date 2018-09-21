package me.voidinvoid.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.Radio;
import me.voidinvoid.utils.ChannelScope;
import me.voidinvoid.utils.FormattingUtils;

public class SeekCommand extends Command {

    SeekCommand() {
        super("seek", "Seeks the current song to a specified position (in seconds)", "<seconds>", ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        String[] args = data.getArgs();

        AudioTrack track = Radio.instance.getOrchestrator().getPlayer().getPlayingTrack();

        if (args.length < 1) {
            data.error("Time to seek to required (in secs)");
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

        Radio.instance.getOrchestrator().seekTrack(time);
        data.success("Seeked track to " + FormattingUtils.getFormattedMsTime(time));
    }
}
