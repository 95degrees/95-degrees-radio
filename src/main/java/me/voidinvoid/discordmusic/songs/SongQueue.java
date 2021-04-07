package me.voidinvoid.discordmusic.songs;

import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import me.voidinvoid.discordmusic.utils.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public abstract class SongQueue extends AudioEventAdapter {

    private static final Random RANDOM = new Random();

    private List<Song> queue;
    private List<Song> songMap;
    private Playlist playlist;
    private SongType queueType;
    private boolean shuffleSongs;

    private MessageEmbed queueCache;

    public SongQueue(Playlist playlist, SongType queueType, boolean shuffleSongs) {
        this.playlist = playlist;
        this.queueType = queueType;
        this.shuffleSongs = shuffleSongs;
    }

    public CompletableFuture<List<Song>> loadSongsAsync() {
        CompletableFuture<List<Song>> files = CompletableFuture.supplyAsync(this::initSongs); //find all files async
        files.whenComplete((l, e) -> {
            if (l == null) return;

            System.out.println(ConsoleColor.BLUE_BACKGROUND + " Playlist " + ConsoleColor.RESET_SPACE + "Found " + l.size() + " songs in " + queueType.getDisplayName() + " queue for playlist " + (playlist == null ? "<null>" : playlist.getName()));

            List<Song> songMap = new ArrayList<>(l); //a clone
            songMap.sort(Comparator.comparing(Song::getLavaIdentifier)); //probably not needed but just in-case

            if (shuffleSongs) Collections.shuffle(l);

            List<Song> allSongs = new ArrayList<>();

            if (queue != null) { //when updating the queue, re-add the network songs...
                allSongs = queue.stream().filter(s -> s instanceof UserSuggestable).collect(Collectors.toList());
            }

            allSongs.addAll(l); //...and then add the rest of the songs to the queue (so network songs are first)

            this.songMap = songMap;

            queue = allSongs;
            queueCache = null;
        });

        return files;
    }

    protected abstract List<Song> initSongs();

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

    public boolean remove(Song song) {
        if (!(song instanceof UserSuggestable)) {
            return false;
        }

        queueCache = null;
        return queue.remove(song);
    }

    public int addSuggestion(Song song) {
        if (!(song instanceof UserSuggestable)) {
            return -1;
        }

        queueCache = null;

        song.setQueue(this);

        int pos = 0; //push the song to the bottom of all suggestions but above regular songs
        for (Song s : queue) {
            if (!(s instanceof UserSuggestable)) break;

            pos++;
        }

        queue.add(pos, song);
        return pos;
    }

    public void addSuggestions(List<Song> songs) {
        songs.forEach(this::addSuggestion);
    }

    public List<Song> getQueue() {
        return queue;
    }

    public List<Song> suggestionsBy(User user) {
        return queue.stream().filter(s -> s instanceof UserSuggestable && ((UserSuggestable) s).getSuggestedBy().equals(user)).collect(Collectors.toList());
    }

    public SongType getQueueType() {
        return queueType;
    }

    public List<Song> getSongMap() {
        return songMap;
    }

    public String getFormattedMap(int page) {
        int pages = (int) Math.ceil((double) songMap.size() / 10D);
        if (page < 1 || page > pages) return null;
        StringBuilder output = new StringBuilder("[Song Listing] [" + page + "/" + pages + "]\n\n");

        int min = (page - 1) * 10;
        int max = Math.min(songMap.size(), min + 10);

        int i = 0;
        for (Song s : songMap) {
            i++;
            if (i - 1 < min || i - 1 >= max) continue;
            String loc = Songs.titleArtist(s);
            output.append(i).append(i < 10 ? "  " : i < 100 ? " " : "").append(": ").append(loc, 0, Math.min(loc.length(), 150)).append("\n");
        }

        String res = Formatting.escape(output.toString());
        return res.substring(0, Math.min(res.length(), 1980));
    }

    public MessageEmbed getFormattedQueue() {
        if (queueCache != null) return queueCache;

        var embed = new EmbedBuilder().setTitle("Song Queue").setColor(Colors.ACCENT_MAIN);

        StringBuilder output = new StringBuilder();

        int i = 0;
        for (Song s : queue) {
            i++;
            var suggestedBy = s instanceof UserSuggestable ? ((UserSuggestable) s).getSuggestedBy() : null;
            var userIcon = "";

            if (suggestedBy != null) {
                var e = Emoji.getOrCreateUserEmoji(suggestedBy, null);

                if (e != null) {
                    userIcon = " " + e.toString() + " ";
                }
            }

            output.append("**").append("#").append(i).append("** - ").append(userIcon).append(Formatting.escape(Songs.titleArtist(s))).append("\n");

            if (i >= 10) break;
        }

        String res = output.toString();
        return queueCache = embed.setDescription(res.substring(0, Math.min(res.length(), 2048))).build();
    }

    public List<Song> getSuggestions() {
        return queue.stream().filter(UserSuggestable.class::isInstance).collect(Collectors.toList());
    }

    public List<Song> clearSuggestions() {
        List<Song> songs = getSuggestions();
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
        queue.add(getSuggestions().size(), song);

        queueCache = null;
    }

    public Playlist getPlaylist() {
        return playlist;
    }
}
