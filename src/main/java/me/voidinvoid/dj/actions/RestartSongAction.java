package me.voidinvoid.dj.actions;

import me.voidinvoid.SongOrchestrator;
import me.voidinvoid.songs.Song;
import net.dv8tion.jda.core.entities.TextChannel;

public class RestartSongAction extends DJAction {

    public RestartSongAction() {
        super("Restart Song", "⏪");
    }

    @Override
    public boolean shouldShow(Song song) {
        return song.getTrack().isSeekable();
    }

    @Override
    public void invoke(SongOrchestrator orch, Song song, TextChannel djChannel) {
        orch.getPlayer().getPlayingTrack().setPosition(0);
    }
}
