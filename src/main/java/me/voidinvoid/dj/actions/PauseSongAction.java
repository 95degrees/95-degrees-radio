package me.voidinvoid.dj.actions;

import me.voidinvoid.SongOrchestrator;
import me.voidinvoid.songs.Song;
import net.dv8tion.jda.core.entities.TextChannel;

public class PauseSongAction extends DJAction {

    public PauseSongAction() {
        super("Play/Pause Song", "⏯");
    }

    @Override
    public boolean shouldShow(Song song) {
        return song.getTrack().isSeekable();
    }

    @Override
    public void invoke(SongOrchestrator orch, Song song, TextChannel djChannel) {
        orch.getPlayer().setPaused(!orch.getPlayer().isPaused());
    }
}
