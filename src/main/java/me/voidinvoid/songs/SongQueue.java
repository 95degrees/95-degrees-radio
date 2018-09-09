package me.voidinvoid.songs;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import me.voidinvoid.utils.ConsoleColor;
import me.voidinvoid.DiscordRadio;
import me.voidinvoid.config.RadioConfig;
import net.dv8tion.jda.core.entities.User;

import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class SongQueue extends AudioEventAdapter {

    private Random random = new Random();

    private List<Song> queue;
    private List<Song> songMap;
    private File directory;
    private SongType queueType;
    private boolean shuffleSongs;
    
    private String queueMapCache;

    public SongQueue(Path directoryLocation, SongType queueType, boolean shuffleSongs) {
        directory = directoryLocation.toFile();
        this.queueType = queueType;
        this.shuffleSongs = shuffleSongs;

        initFiles();

        if (RadioConfig.config.liveFileUpdates) listenForChanges();
    }

    private void listenForChanges() {
        Thread t = new Thread(() -> {
            try {
                WatchService service = FileSystems.getDefault().newWatchService();
                directory.toPath().register(service, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE/*, StandardWatchEventKinds.ENTRY_MODIFY*/);

                while (DiscordRadio.isRunning) {
                    WatchKey wk = service.take();
                    System.out.println(ConsoleColor.WHITE + ConsoleColor.PURPLE_BACKGROUND + " Directory change detected " + ConsoleColor.RESET);

                    /*for (WatchEvent<?> e : wk.pollEvents()) {
                        System.out.println(e.kind().name());
                        //if (StandardWatchEventKinds.OVERFLOW.equals(e.kind())) continue;
                    }*/

                    initFiles();

                    if (!wk.reset()) break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void initFiles() {
        List<Song> songs = new ArrayList<>();
        List<Song> songMap = new ArrayList<>();

        File[] files = directory.listFiles();
        if (files == null) return;

        for (File f : files) {
            Song s = new FileSong(queueType, f);
            s.setQueue(this);
            songs.add(s);
            songMap.add(s);
            System.out.println("        ├─ " + ConsoleColor.BLUE_BACKGROUND + " SONG " + ConsoleColor.RESET_SPACE + f.getName());
        }

        this.songMap = new ArrayList<>(songs);
        if (shuffleSongs) Collections.shuffle(songs);
        List<Song> allSongs = new ArrayList<>();
        if (queue != null) {
             allSongs = queue.stream().filter(s -> s instanceof NetworkSong).collect(Collectors.toList());
        }
        allSongs.addAll(songs);
        queue = allSongs;
        queueMapCache = null;
        this.songMap = songMap;
    }

    public Song getNextAndRemove() {
        if (queue.size() == 0) return null;

        Song song = queue.get(0);
        queue.remove(0);
        
        queueMapCache = null;

        return song;
    }

    public Song getNextAndMoveToEnd() {
        Song song = getNextAndRemove();
        if (song.isPersistent()) queue.add(song);
        
        queueMapCache = null;

        return song;
    }

    public Song getRandom() {
        queueMapCache = null;
        return queue.size() == 0 ? null : queue.get(random.nextInt(queue.size()));
    }

    public boolean remove(NetworkSong song) {
        queueMapCache = null;
        return queue.remove(song);
    }

    public int addNetworkSong(NetworkSong song) {
        queueMapCache = null;
        
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
        if (queueMapCache != null) return queueMapCache;

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
            /*if (s instanceof NetworkSong) {
                NetworkSong ns = ((NetworkSong) s);
                if (ns.getSuggestedBy() != null) output.append(" [").append(ns.getSuggestedBy().getName()).append("]");
            }*/
            output.append("\n");
            if (i >= 10) break;
        }

        String res = output.toString();
        return queueMapCache = res.substring(0, Math.min(res.length(), 1900));
    }

    public void clearNetworkTracks() {
        queue = queue.stream().filter(s -> !(s instanceof NetworkSong)).collect(Collectors.toList());
    }
}
