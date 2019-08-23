package me.voidinvoid.discordmusic.dj.actions;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.SongOrchestrator;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class RestartSongAction extends DJAction {

    public RestartSongAction() {
        super("Restart Song", "⏪");
    }

    @Override
    public boolean shouldShow(AudioTrack track) {
        return track.isSeekable();
    }

    @Override
    public void invoke(SongOrchestrator orch, AudioTrack track, TextChannel djChannel, User invoker) {
        orch.seekTrack(0);
    }
}
