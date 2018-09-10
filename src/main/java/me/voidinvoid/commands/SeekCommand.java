package me.voidinvoid.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.DiscordRadio;
import me.voidinvoid.utils.FormattingUtils;

public class SeekCommand extends Command {

    public SeekCommand() {
        super("seek", "Seeks the current song to a specified position (in seconds)", "<seconds>", CommandScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        String[] args = data.getArgs();

        AudioTrack track = DiscordRadio.instance.getOrchestrator().getPlayer().getPlayingTrack();

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

        track.setPosition(time);
        data.success("Seeked track to " + FormattingUtils.getFormattedMsTime(time));
     }
}
