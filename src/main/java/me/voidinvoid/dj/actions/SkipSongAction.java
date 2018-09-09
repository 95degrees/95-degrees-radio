package me.voidinvoid.dj.actions;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.SongOrchestrator;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

public class SkipSongAction extends DJAction {

    public SkipSongAction() {
        super("Skip Song", "⏩");
    }

    @Override
    public void invoke(SongOrchestrator orch, AudioTrack track, TextChannel djChannel, User invoker) {
        orch.playNextSong();
    }
}
