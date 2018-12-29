package me.voidinvoid.discordmusic.songs;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import me.voidinvoid.discordmusic.utils.ConsoleColor;
import me.voidinvoid.discordmusic.utils.FormattingUtils;
import net.dv8tion.jda.core.entities.User;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SongQueue extends AudioEventAdapter {

    private static final Random RANDOM = new Random();

    private List<Song> queue;
    private List<Song> songMap;
    private Playlist playlist;
    private Path directory;
    private SongType queueType;
    private boolean shuffleSongs;

    private String queueCache;

    public SongQueue(Playlist playlist, Path directory, SongType queueType, boolean shuffleSongs) {
        this.playlist = playlist;
        this.directory = directory;
        this.queueType = queueType;
        this.shuffleSongs = shuffleSongs;
    }

    public CompletableFuture<List<Song>> loadSongsAsync() {
        CompletableFuture<List<Song>> files = CompletableFuture.supplyAsync(this::initSongs); //find all files async
        files.whenComplete((l, e) -> {
            if (l == null) return;

            System.out.println(ConsoleColor.BLUE_BACKGROUND + " PLAYLIST " + ConsoleColor.RESET_SPACE + "Found " + l.size() + " songs in playlist " + (playlist == null ? "<null>" : playlist.getName()));

            List<Song> songMap = new ArrayList<>(l); //a clone
            songMap.sort(Comparator.comparing(Song::getFullLocation)); //probably not needed but just in-case

            if (shuffleSongs) Collections.shuffle(l);

            List<Song> allSongs = new ArrayList<>();

            if (queue != null) { //when updating the queue, re-add the network songs...
                allSongs = queue.stream().filter(s -> s instanceof NetworkSong).collect(Collectors.toList());
            }

            allSongs.addAll(l); //...and then add the rest of the songs to the queue (so network songs are first)

            this.songMap = songMap;

            queue = allSongs;
            queueCache = null;
        });

        return files;
    }

    private List<Song> initSongs() {
        try {
            return Files.walk(directory, 1)
                    .filter(f -> !Files.isDirectory(f))
            .map(f -> new FileSong(queueType, f, this)).collect(Collectors.toList());

        } catch (Exception ex) {
            System.err.println("Error fetching songs for playlist");
            ex.printStackTrace();

            return null;
        }
    }

    public Song getNextAndRemove() {
        if (queue.size() == 0) return null;

        Song song = queue.get(0);
        queue.remove(0);

        queueCache = null;

        return song;
    }

    public Song getNextAndMoveToEnd() {
        if (queue.size() == 0) return null;

        Song song = getNextAndRemove();
        if (song.isPersistent()) queue.add(song);

        queueCache = null;

        return song;
    }

    public Song getRandom() {
        queueCache = null;
        return queue.size() == 0 ? null : queue.get(RANDOM.nextInt(queue.size()));
    }

    public boolean remove(NetworkSong song) {
        queueCache = null;
        return queue.remove(song);
    }

    public int addNetworkSong(NetworkSong song) {
        queueCache = null;

        song.setQueue(this);

        int pos = 0; //push the song to the bottom of all network songs but above regular songs
        for (Song s : queue) {
            if (!(s instanceof NetworkSong)) break;

            pos++;
        }

        queue.add(pos, song);
        return pos;
    }

    public void addNetworkSongs(List<NetworkSong> songs) {
        songs.forEach(this::addNetworkSong);
    }

    public List<Song> getQueue() {
        return queue;
    }

    public List<Song> suggestionsBy(User user) {
        return queue.stream().filter(s -> s instanceof NetworkSong && ((NetworkSong) s).getSuggestedBy().equals(user)).collect(Collectors.toList());
    }

    public Path getDirectory() {
        return directory;
    }

    public List<Song> getSongMap() {
        return songMap;
    }

    public String getFormattedMap(int page) {
        int pages = (int) Math.ceil((double) songMap.size() / 10D);
        if (page < 1 || page > pages) return null;
        StringBuilder output = new StringBuilder("[Song listing] [" + page + "/" + pages + "]\n\n");

        int min = (page - 1) * 10;
        int max = Math.min(songMap.size(), min + 10);

        int i = 0;
        for (Song s : songMap) {
            i++;
            if (i - 1 < min || i - 1 >= max) continue;
            String loc = s.getFileName();
            output.append(i).append(i < 10 ? "  " : i < 100 ? " " : "").append(": ").append(loc, 0, Math.min(loc.length(), 150)).append("\n");
        }

        String res = FormattingUtils.escapeMarkup(output.toString());
        return res.substring(0, Math.min(res.length(), 1980));
    }

    public String getFormattedQueue() {
        if (queueCache != null) return queueCache;

        StringBuilder output = new StringBuilder("[Song queue]\n\n");

        int i = 0;
        for (Song s : queue) {
            i++;
            output.append(i).append(i < 10 ? " " : "").append(": ");
            if (s.getTrack() != null) {
                output.append(s.getTrack().getInfo().title).append(" (").append(s.getTrack().getInfo().author).append(")");
            } else {
                boolean addedDesc = false;
                try {
                    Mp3File m = new Mp3File(s.getFullLocation());
                    if (m.hasId3v2Tag()) {
                        ID3v2 tag = m.getId3v2Tag();
                        output.append(tag.getArtist()).append(" - ").append(tag.getTitle());
                        addedDesc = true;
                    }
                } catch (Exception ignored) {
                }

                if (!addedDesc) output.append(s.getFileName());
            }

            output.append("\n");
            if (i >= 10) break;
        }

        String res = output.toString();
        return queueCache = res.substring(0, Math.min(res.length(), 1900));
    }

    public List<NetworkSong> getNetworkSongs() {
        return queue.stream().filter(NetworkSong.class::isInstance).map(NetworkSong.class::cast).collect(Collectors.toList());
    }

    public List<NetworkSong> clearNetworkSongs() {
        List<NetworkSong> songs = getNetworkSongs();
        queue.removeAll(songs);

        return songs;
    }

    public void shuffleQueue() {
        Collections.shuffle(queue);
        queueCache = null;
    }

    public void moveSongToFront(Song song) {
        if (song.getQueue() != this) return;

        queue.remove(song);
        queue.add(getNetworkSongs().size(), song);

        queueCache = null;
    }

    public Playlist getPlaylist() {
        return playlist;
    }
}
