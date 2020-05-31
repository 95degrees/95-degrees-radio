package me.voidinvoid.discordmusic.dj.actions;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.SongOrchestrator;
import me.voidinvoid.discordmusic.rpc.RPCSocketManager;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class PlayJingleAction extends DJAction {

    public PlayJingleAction() {
        super("Skip and Play Jingle", "🎹", RPCSocketManager.CLIENT_CONTROL_PLAY_JINGLE);
    }

    @Override
    public String invoke(SongOrchestrator orch, AudioTrack track, TextChannel djChannel, User invoker) {
        orch.setTimeUntilJingle(-1);
        orch.playNextSong();

        return "Playing jingle";
    }
}
