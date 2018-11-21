package me.voidinvoid.discordmusic.tasks.types;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.SongOrchestrator;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.tasks.ParameterList;
import me.voidinvoid.discordmusic.tasks.RadioTaskExecutor;

import java.util.List;
import java.util.stream.Collectors;

public class PlaySpecialTask extends RadioTaskExecutor {

    @Override
    public void runTask(SongOrchestrator orch, ParameterList params) {
        String song = params.get("song_name", String.class);
        boolean force = params.get("play_instantly", Boolean.class);
        String listeningTo = params.get("listening_to", String.class);

        List<Song> foundSongs = orch.getSpecialQueue().getQueue().stream().filter(s -> s.getFileName().equalsIgnoreCase(song)).collect(Collectors.toList());

        if (foundSongs.size() == 0) {
            System.out.println("TASK: couldn't find song_name: " + song);
            return;
        }

        if (listeningTo != null) {
            Radio.instance.getStatusManager().addSongOverride(foundSongs.get(0), listeningTo);
        }

        if (force) {
            orch.playSong(foundSongs.get(0));
        } else {
            orch.getAwaitingSpecialSongs().add(foundSongs.get(0));
        }
    }
}
