package me.voidinvoid.dj.actions;

import me.voidinvoid.SongOrchestrator;
import me.voidinvoid.songs.Song;
import net.dv8tion.jda.core.entities.TextChannel;

public class PlayJingleAction extends DJAction {

    public PlayJingleAction() {
        super("Skip and Play Jingle", "🎹");
    }

    @Override
    public void invoke(SongOrchestrator orch, Song song, TextChannel djChannel) {

    }
}
