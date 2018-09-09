package me.voidinvoid.dj.actions;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.SongOrchestrator;
import net.dv8tion.jda.core.entities.TextChannel;

public class RestartSongAction extends DJAction {

    public RestartSongAction() {
        super("Restart Song", "⏪");
    }

    @Override
    public boolean shouldShow(AudioTrack track) {
        return track.isSeekable();
    }

    @Override
    public void invoke(SongOrchestrator orch, AudioTrack track, TextChannel djChannel) {
        orch.getPlayer().getPlayingTrack().setPosition(0);
    }
}
