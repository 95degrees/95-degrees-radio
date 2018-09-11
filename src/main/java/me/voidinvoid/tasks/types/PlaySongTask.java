package me.voidinvoid.tasks.types;

import me.voidinvoid.Radio;
import me.voidinvoid.SongOrchestrator;
import me.voidinvoid.songs.SongQueue;
import me.voidinvoid.songs.Song;
import me.voidinvoid.suggestions.SuggestionQueueMode;
import me.voidinvoid.tasks.ParameterList;
import me.voidinvoid.tasks.RadioTaskExecutor;

import java.util.List;
import java.util.stream.Collectors;

public class PlaySongTask extends RadioTaskExecutor {

    private boolean jingle;

    public PlaySongTask(boolean jingle) {
        this.jingle = jingle;
    }

    @Override
    public void runTask(SongOrchestrator orch, ParameterList params) {
        String song = params.get("song_name", String.class);
        boolean force = params.get("play_instantly", Boolean.class);

        if (!jingle && params.get("remote", Boolean.class)) {
            Radio.instance.getSuggestionManager().addSuggestion(song, null, null, false, force ? SuggestionQueueMode.PLAY_INSTANTLY : SuggestionQueueMode.PUSH_TO_START);
            return;
        }

        if (jingle && song == null) {
            orch.setTimeUntilJingle(-1);

            if (force) {
                orch.getPlayer().setPaused(false);
                orch.getPlayer().stopTrack();
                orch.playNextSong();
            }

            return;
        }

        SongQueue queue = jingle ? orch.getActivePlaylist().getJingles() : orch.getActivePlaylist().getSongs();

        List<Song> foundSongs = queue.getQueue().stream().filter(s -> s.getLocation().equalsIgnoreCase(song)).collect(Collectors.toList());

        if (foundSongs.size() == 0) {
            System.out.println("TASK: couldn't find song_name: " + song);
            return;
        }

        if (jingle) {
            orch.playSong(foundSongs.get(0));
            return;
        }

        queue.getQueue().remove(foundSongs.get(0));
        queue.getQueue().add(0, foundSongs.get(0));

        if (force) {
            orch.getPlayer().setPaused(false);
            orch.getPlayer().stopTrack();
            orch.playNextSong();
        }
    }
}
