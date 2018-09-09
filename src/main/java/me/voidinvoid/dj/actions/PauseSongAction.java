package me.voidinvoid.dj.actions;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.SongOrchestrator;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

public class PauseSongAction extends DJAction {

    public PauseSongAction() {
        super("Play/Pause Song", "⏯");
    }

    @Override
    public boolean shouldShow(AudioTrack track) {
        return track.isSeekable();
    }

    @Override
    public void invoke(SongOrchestrator orch, AudioTrack track, TextChannel djChannel, User invoker) {
        orch.getPlayer().setPaused(!orch.getPlayer().isPaused());
    }
}
