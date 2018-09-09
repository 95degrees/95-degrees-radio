package me.voidinvoid.dj.actions;

import me.voidinvoid.SongOrchestrator;
import me.voidinvoid.songs.Song;
import net.dv8tion.jda.core.entities.TextChannel;

public class SkipSongAction extends DJAction {

    public SkipSongAction() {
        super("Skip Song", "⏩");
    }

    @Override
    public void invoke(SongOrchestrator orch, Song song, TextChannel djChannel) {
        orch.playNextSong();
    }
}
