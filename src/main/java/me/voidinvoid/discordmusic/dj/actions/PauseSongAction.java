package me.voidinvoid.discordmusic.dj.actions;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.SongOrchestrator;
import me.voidinvoid.discordmusic.rpc.RPCSocketManager;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

public class PauseSongAction extends DJAction {

    public PauseSongAction() {
        super("Play/Pause Song", "⏯", RPCSocketManager.CLIENT_CONTROL_PLAY_PAUSE_SONG, 0);
    }

    @Override
    public String invoke(SongOrchestrator orch, AudioTrack track, TextChannel djChannel, User invoker, ButtonClickEvent event) {
        orch.setPaused(!orch.getPlayer().isPaused(), event);

        return null;
    }
}
