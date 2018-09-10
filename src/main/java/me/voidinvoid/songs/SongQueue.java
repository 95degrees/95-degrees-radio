package me.voidinvoid.songs;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import me.voidinvoid.utils.ConsoleColor;
import net.dv8tion.jda.core.entities.User;

import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SongQueue extends AudioEventAdapter {

    private static final Random RANDOM = new Random();

    private List<Song> queue;
    private List<Song> songMap;
    private File directory;
    private SongType queueType;
    private boolean shuffleSongs;
    
    private String queueCache;

    public SongQueue(Path directoryLocation, SongType queueType, boolean shuffleSongs) {
        directory = directoryLocation.toFile();
        this.queueType = queueType;
        this.shuffleSongs = shuffleSongs;

        //if (RadioConfig.config.liveFileUpdates) listenForChanges(); todo maybe reimplement
    }

    public CompletableFuture<List<Song>> loadSongsAsync() {
        CompletableFuture<List<Song>> files = CompletableFuture.supplyAsync(this::initSongs); //find all files async
        files.whenComplete((l, e) -> {

            List<Song> songMap = new ArrayList<>(l); //a clone
            songMap.sort(Comparator.comparing(Song::getIdentifier)); //probably not needed but just in-case

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
        List<Song> songs = new ArrayList<>();

        File[] files = directory.listFiles();
        if (files == null) return songs;

        for (File f : files) {
            Song s = new FileSong(queueType, f);
            s.setQueue(this);
            songs.add(s);
            System.out.println(ConsoleColor.BLUE_BACKGROUND + " SONG " + ConsoleColor.RESET_SPACE + f.getName());
        }

        return songs;
    }

    public Song getNextAndRemove() {
        if (queue.size() == 0) return null;

        Song song = queue.get(0);
        queue.remove(0);
        
        queueCache = null;

        return song;
    }

    public Song getNextAndMoveToEnd() {
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

        int pos = 0;
        for (Song s : queue) {
            if (!(s instanceof NetworkSong)) break;

            pos++;
        }

        queue.add(pos, song);
        return pos;
    }

    public List<Song> getQueue() {
        return queue;
    }

    public List<Song> suggestionsBy(User user) {
        return queue.stream().filter(s -> s instanceof NetworkSong && ((NetworkSong) s).getSuggestedBy().equals(user)).collect(Collectors.toList());
    }

    public File getDirectory() {
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
            String loc = s.getLocation();
            output.append(i).append(i < 10 ? "  " : i < 100 ? " " : "").append(": ").append(loc, 0, Math.min(loc.length(), 150)).append("\n");
        }

        String res = output.toString();
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
                    Mp3File m = new Mp3File(s.getIdentifier());
                    if (m.hasId3v2Tag()) {
                        ID3v2 tag = m.getId3v2Tag();
                        output.append(tag.getArtist()).append(" - ").append(tag.getTitle());
                        addedDesc = true;
                    }
                } catch (Exception ignored) {}

                if (!addedDesc) output.append(s.getLocation());
            }

            output.append("\n");
            if (i >= 10) break;
        }

        String res = output.toString();
        return queueCache = res.substring(0, Math.min(res.length(), 1900));
    }

    public void clearNetworkTracks() {
        queue = queue.stream().filter(s -> !(s instanceof NetworkSong)).collect(Collectors.toList());
    }
}
