package me.voidinvoid.discordmusic.dj.actions;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.SongOrchestrator;
import me.voidinvoid.discordmusic.rpc.RPCSocketManager;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

public class RestartSongAction extends DJAction {

    public RestartSongAction() {
        super("Restart Song", "⏪", RPCSocketManager.CLIENT_CONTROL_RESTART_SONG, 0);
    }

    @Override
    public boolean shouldShow(AudioTrack track) {
        return track.isSeekable();
    }

    @Override
    public String invoke(SongOrchestrator orch, AudioTrack track, TextChannel djChannel, User invoker, ButtonClickEvent event) {
        orch.seekTrack(0);

        return "Restarting track";
    }
}
