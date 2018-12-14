package me.voidinvoid.discordmusic.tasks.types;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.SongOrchestrator;
import me.voidinvoid.discordmusic.songs.Playlist;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.RadioPlaylist;
import me.voidinvoid.discordmusic.songs.SongQueue;
import me.voidinvoid.discordmusic.suggestions.SuggestionQueueMode;
import me.voidinvoid.discordmusic.tasks.ParameterList;
import me.voidinvoid.discordmusic.tasks.RadioTaskExecutor;

import java.util.List;
import java.util.stream.Collectors;

public class PlaySongTask extends RadioTaskExecutor {

    private boolean jingle;

    PlaySongTask(boolean jingle) {
        this.jingle = jingle;
    }

    @Override
    public void runTask(SongOrchestrator orch, ParameterList params) {
        String song = params.get("song_name", String.class);
        boolean force = params.get("play_instantly", Boolean.class);

        Playlist playlist = orch.getActivePlaylist();
        if (!(playlist instanceof RadioPlaylist)) return;

        if (!jingle && params.get("remote", Boolean.class)) {
            Radio.instance.getSuggestionManager().addSuggestion(song, null, null, null, false, force ? SuggestionQueueMode.PLAY_INSTANTLY : SuggestionQueueMode.PUSH_TO_START);
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

        SongQueue queue = jingle ? ((RadioPlaylist) playlist).getJingles() : ((RadioPlaylist) playlist).getSongs();

        List<Song> foundSongs = queue.getQueue().stream().filter(s -> s.getFileName().equalsIgnoreCase(song)).collect(Collectors.toList());

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
