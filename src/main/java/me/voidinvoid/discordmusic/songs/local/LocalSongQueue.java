package me.voidinvoid.discordmusic.songs.local;

import me.voidinvoid.discordmusic.songs.Playlist;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.SongQueue;
import me.voidinvoid.discordmusic.songs.SongType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class LocalSongQueue extends SongQueue {

    private Path directory;

    public LocalSongQueue(Path directory, Playlist playlist, SongType queueType, boolean shuffleSongs) {
        super(playlist, queueType, shuffleSongs);

        this.directory = directory;
    }

    public Path getDirectory() {
        return directory;
    }

    @Override
    protected List<Song> initSongs() {
        try {
            return Files.walk(directory, 1)
                    .filter(f -> !Files.isDirectory(f))
                    .map(f -> new FileSong(getQueueType(), f, this)).collect(Collectors.toList());

        } catch (Exception ex) {
            System.err.println("Error fetching songs for playlist");
            ex.printStackTrace();

            return null;
        }
    }
}
