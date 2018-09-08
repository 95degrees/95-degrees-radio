package me.voidinvoid.tasks.types;

import me.voidinvoid.SongOrchestrator;
import me.voidinvoid.songs.Song;
import me.voidinvoid.tasks.ParameterList;
import me.voidinvoid.tasks.RadioTaskExecutor;

import java.util.List;
import java.util.stream.Collectors;

public class PlaySpecialTask extends RadioTaskExecutor {

    @Override
    public void runTask(SongOrchestrator orch, ParameterList params) {
        String song = params.get("song_name", String.class);
        boolean force = params.get("play_instantly", Boolean.class);
        String listeningTo = params.get("listening_to", String.class);

        List<Song> foundSongs = orch.getSpecialQueue().getQueue().stream().filter(s -> s.getLocation().equalsIgnoreCase(song)).collect(Collectors.toList());

        if (foundSongs.size() == 0) {
            System.out.println("TASK: couldn't find song_name: " + song);
            return;
        }

        if (listeningTo != null) {
            orch.getNowPlayingSongOverrides().put(foundSongs.get(0), listeningTo);
        }

        if (force) {
            orch.playSong(foundSongs.get(0));
        } else {
            orch.getAwaitingSpecialSongs().add(foundSongs.get(0));
        }
    }
}
