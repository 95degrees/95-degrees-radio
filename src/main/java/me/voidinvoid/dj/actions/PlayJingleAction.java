package me.voidinvoid.dj.actions;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.SongOrchestrator;
import net.dv8tion.jda.core.entities.TextChannel;

public class PlayJingleAction extends DJAction {

    public PlayJingleAction() {
        super("Skip and Play Jingle", "🎹");
    }

    @Override
    public void invoke(SongOrchestrator orch, AudioTrack track, TextChannel djChannel) {
        orch.setTimeUntilJingle(0);
        orch.playNextSong();
    }
}
