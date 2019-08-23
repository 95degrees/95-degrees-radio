package me.voidinvoid.discordmusic.dj.actions;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.SongOrchestrator;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class PlayJingleAction extends DJAction {

    public PlayJingleAction() {
        super("Skip and Play Jingle", "🎹");
    }

    @Override
    public void invoke(SongOrchestrator orch, AudioTrack track, TextChannel djChannel, User invoker) {
        orch.setTimeUntilJingle(-1);
        orch.playNextSong();
    }
}
