package me.voidinvoid;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import me.voidinvoid.audio.AudioPlayerSendHandler;
import me.voidinvoid.audio.twitch.TwitchKrakenStreamAudioSourceManager;
import me.voidinvoid.config.RadioConfig;
import me.voidinvoid.events.NetworkSongError;
import me.voidinvoid.events.SongEventListener;
import me.voidinvoid.songs.*;
import me.voidinvoid.tasks.RadioTaskComposition;
import me.voidinvoid.tasks.TaskManager;
import me.voidinvoid.utils.AlbumArtUtils;
import me.voidinvoid.utils.Colors;
import me.voidinvoid.utils.ConsoleColor;
import me.voidinvoid.utils.FormattingUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.EventListener;

import java.awt.*;
import java.io.File;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class SongOrchestrator extends AudioEventAdapter implements EventListener {

    public static final int JINGLE_FREQUENCY = 3;
    public static final int MAX_SONG_LENGTH = 300000;
    public static final int USER_QUEUE_LIMIT = 3;

    private List<SongPlaylist> playlists;
    private SongPlaylist activePlaylist;
    private int timeUntilJingle = 0;
    private DefaultAudioPlayerManager manager;
    private AudioPlayer player;
    private AudioPlayerSendHandler audioSendHandler;

    @Deprecated
    private TextChannel radioChannel;

    @Deprecated
    public TextChannel djChannel_temp; //todo temp

    private JDA jda;
    private File playlistsRoot;
    private boolean suggestionsEnabled = true;
    private SongQueue specialQueue;

    private DiscordRadio radio;

    private List<Song> awaitingSpecialSongs = new ArrayList<>();

    private Map<Song, String> nowPlayingSongOverrides = new HashMap<>();

    private Map<Long, SongSearchPlaylist> searchPlaylists = new HashMap<>();

    private List<SongEventListener> songEventListeners = new ArrayList<>();

    public SongOrchestrator(DiscordRadio radio, RadioConfig config) {

        this.radio = radio;
        jda = radio.getJda();

        this.playlistsRoot = new File(config.locations.playlists);

        loadPlaylists();

        manager = new DefaultAudioPlayerManager();

        AudioSourceManagers.registerLocalSource(manager);
        manager.registerSourceManager(new YoutubeAudioSourceManager(true));
        manager.registerSourceManager(new SoundCloudAudioSourceManager());
        manager.registerSourceManager(new BandcampAudioSourceManager());
        manager.registerSourceManager(new VimeoAudioSourceManager());
        manager.registerSourceManager(new TwitchKrakenStreamAudioSourceManager());
        manager.registerSourceManager(new BeamAudioSourceManager());
        manager.registerSourceManager(new HttpAudioSourceManager());

        player = manager.createPlayer();
        player.addListener(this);

        player.setVolume(50);

        jda.addEventListener(this);

        //todo temp
        djChannel_temp = jda.getTextChannelById(RadioConfig.config.channels.djChat);

        audioSendHandler = new AudioPlayerSendHandler(player);
    }

    public void registerSongEventListener(SongEventListener listener) {
        songEventListeners.add(listener);
    }

    public JDA getJda() {
        return jda;
    }

    public DefaultAudioPlayerManager getAudioManager() {
        return manager;
    }

    public AudioPlayerSendHandler getAudioSendHandler() {
        return audioSendHandler;
    }

    public List<SongPlaylist> getPlaylists() {
        return playlists;
    }

    public int getTimeUntilJingle() {
        return timeUntilJingle;
    }

    public void setTimeUntilJingle(int timeUntilJingle) {
        this.timeUntilJingle = timeUntilJingle;
    }

    public SongPlaylist getActivePlaylist() {
        return activePlaylist;
    }

    public void setActivePlaylist(SongPlaylist activePlaylist) {
        if (this.activePlaylist != null) {
            this.activePlaylist.getSongs().clearNetworkTracks();
        }

        songEventListeners.forEach(l -> l.onPlaylistChange(this.activePlaylist, activePlaylist));

        this.activePlaylist = activePlaylist;
    }

    public AudioPlayer getPlayer() {
        return player;
    }

    public Song getCurrentSong() {
        return player.getPlayingTrack().getUserData(Song.class);
    }

    public void loadPlaylists() {
        System.out.println("Loading special queue...");
        specialQueue = new SongQueue(Paths.get(RadioConfig.config.locations.specialPlaylist), SongType.SPECIAL, false);

        System.out.println("Loading playlists...");
        File[] playlistFiles = playlistsRoot.listFiles(File::isDirectory);

        if (playlistFiles == null) {
            System.out.println(ConsoleColor.RED + "Playlist folder file listing is null" + ConsoleColor.RESET);
            return;
        }

        playlists = new ArrayList<>();

        for (File f : playlistFiles) {
            System.out.println(ConsoleColor.CYAN_BACKGROUND_BRIGHT + ConsoleColor.BLACK_BRIGHT + " PLAYLIST " + ConsoleColor.RESET_SPACE + f.getName());
            SongPlaylist s = new SongPlaylist(f);
            playlists.add(s);

            if (s.isDefault()) activePlaylist = s;
        }

        if (playlists.size() == 0) {
            System.out.println(ConsoleColor.RED + "No playlists found!" + ConsoleColor.RESET);
            return;
        }

        if (activePlaylist == null) {
            activePlaylist = playlists.get(0);
            System.out.println(ConsoleColor.YELLOW + "Warning: no default song directory found. Defaulted to " + activePlaylist.getName() + ConsoleColor.RESET);
        }

        activePlaylist.awaitLoad();
    }

    public void playNextSong() {
        playNextSong(false, true);
    }

    public void playNextSong(boolean skipJingle, boolean decrementJingleCounter) {
        if (activePlaylist.isJinglesEnabled() && decrementJingleCounter) timeUntilJingle--;

        /*if (karaokeAudioListener.isRecording()) {
            System.out.println("SAVING KARAOKE RECORDING");
            karaokeAudioListener.stopRecording(true);
        }*/

        if (!skipJingle && (timeUntilJingle < 0)) {
            timeUntilJingle = JINGLE_FREQUENCY;
            playSong(activePlaylist.getJingles().getRandom());
            return;
        }

        if (awaitingSpecialSongs.size() > 0) {
            playSong(awaitingSpecialSongs.remove(0));
            return;
        }
        //playingJingle = false;

        playSong(activePlaylist.getSongs().getNextAndMoveToEnd());
    }

    public void playSong(final Song song) {
        System.out.println(ConsoleColor.WHITE_BACKGROUND + ConsoleColor.BLACK + " NOW PLAYING " + ConsoleColor.RESET_SPACE + ConsoleColor.WHITE_BOLD + song.getLocation() + ConsoleColor.RESET);
        System.out.println("              Jingle after " + timeUntilJingle + " more songs");

        if (song.getTrack() != null) {
            player.playTrack(song.getTrack());
            return;
        }

        manager.loadItem(song.getIdentifier(), new AudioLoadResultHandler() {
            public void trackLoaded(AudioTrack track) {
                track.setUserData(song);
                player.playTrack(track);
            }

            public void playlistLoaded(AudioPlaylist p) {
            }

            public void noMatches() {
                System.out.println("No match found for file");
                playNextSong(false, false);
            }

            public void loadFailed(FriendlyException e) {
                System.out.println("Load failed for file: (ID) " + song.getIdentifier());
                e.printStackTrace();
                playNextSong(false, false);

                songEventListeners.forEach(l -> l.onSongLoadError(song, e));
            }
        });
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        if (RadioConfig.config.useStatus) jda.getPresence().setIdle(true);
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        if (RadioConfig.config.useStatus) jda.getPresence().setIdle(false);
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        final Song song = track.getUserData(Song.class);
        String overrideListeningTo = nowPlayingSongOverrides.remove(song);

        songEventListeners.forEach(l -> l.onSongStart(song, track, player, timeUntilJingle));

        if (song.getType() != SongType.SONG) {
            if (RadioConfig.config.useStatus) {
                if (overrideListeningTo != null) {
                    jda.getPresence().setGame(Game.listening(overrideListeningTo));
                } else {
                    jda.getPresence().setGame(null);
                }
            }
        } else {
            if (RadioConfig.config.useStatus) {
                if (overrideListeningTo != null) {
                    jda.getPresence().setGame(Game.listening(overrideListeningTo));
                } else if (track.getInfo().isStream) {
                    jda.getPresence().setGame(Game.streaming(track.getInfo().title, track.getInfo().uri));
                } else {
                    jda.getPresence().setGame(Game.listening(song instanceof NetworkSong ? track.getInfo().title : (track.getInfo().author + " - " + track.getInfo().title)));
                }
            }
        }
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext || endReason == AudioTrackEndReason.REPLACED) {
            Song song = track.getUserData(Song.class);
            songEventListeners.forEach(l -> l.onSongEnd(song, track));
        }

        if (endReason.mayStartNext) {
            playNextSong();
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        playNextSong(false, false);
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        setTimeUntilJingle(-1);
        playNextSong();
    }

    public void addLoadedNetworkTrack(User suggestedBy, AudioTrack track, boolean allowStream, boolean playInstantly, boolean pushToStart) {
        System.out.println("Found network track '" + track.getInfo().title + "', suggested by " + suggestedBy);

        if (!allowStream && track instanceof LocalAudioTrack) {
            System.out.println(suggestedBy + " tried to play a local track - disallowed");
            return;
        }

        NetworkSong song = new NetworkSong(SongType.SONG, track, suggestedBy);

        if (!allowStream) {
            final NetworkSongError error;

            if (!suggestionsEnabled) {
                error = NetworkSongError.SONG_SUGGESTIONS_DISABLED;
            } else if (activePlaylist.getSongs().suggestionsBy(suggestedBy).size() >= USER_QUEUE_LIMIT) {
                error = NetworkSongError.QUEUE_LIMIT_REACHED;
            } else if (track.getInfo().isStream) {
                error = NetworkSongError.IS_STREAM;
            } else if (track.getDuration() > MAX_SONG_LENGTH) {
                error = NetworkSongError.EXCEEDS_LENGTH_LIMIT;
            } else {
                error = null;
            }

            if (error != null) {
                songEventListeners.forEach(l -> l.onNetworkSongQueueError(song, track, suggestedBy, error));
                return;
            }
        }

        final int index;

        if (pushToStart || playInstantly) {
            index = 0;
        } else {
            index = activePlaylist.getSongs().addNetworkSong(song);
        }

        songEventListeners.forEach(l -> l.onNetworkSongQueued(song, track, suggestedBy, index));

        if (pushToStart || playInstantly) {
            activePlaylist.getSongs().getQueue().add(0, song);

            if (playInstantly) {
                player.setPaused(false);
                player.stopTrack();
                playNextSong(true, false);
            }
        }

        System.out.println("Network track is in the queue: #" + (index + 1));
    }

    public void addNetworkTrack(User author, TextChannel channel, String identifier, boolean allowStream, boolean playInstantly, boolean pushToStart, boolean alertOnFailure) {
        if (identifier.toLowerCase().startsWith("yt:")) identifier = "ytsearch:" + identifier.substring(3);

        manager.loadItem(identifier, new AudioLoadResultHandler() {
            public void trackLoaded(AudioTrack track) {
                addLoadedNetworkTrack(author, track, allowStream, playInstantly, pushToStart);
            }

            public void playlistLoaded(AudioPlaylist p) {
                new SongSearchPlaylist(p, author, djChannel_temp.equals(channel)).sendMessage(channel);
            }

            public void noMatches() {
                if (alertOnFailure) {
                    commandError(channel, author, "No song results found");
                }
            }

            public void loadFailed(FriendlyException e) {
            }
        });
    }

    private void commandSuccess(TextChannel channel, User author, String error) {
        channel.sendMessage(new EmbedBuilder().setTitle("Command Successful").setColor(Color.GREEN).setDescription(error).setFooter(author.getName(), author.getAvatarUrl()).setTimestamp(OffsetDateTime.now()).build()).queue();
    }

    private void commandError(TextChannel channel, User author, String error) {
        channel.sendMessage(new EmbedBuilder().setTitle("Command Error").setColor(Color.RED).setDescription(error).setFooter(author.getName(), author.getAvatarUrl()).setTimestamp(OffsetDateTime.now()).build()).queue();
    }

    private void commandSuccess(Message message, User author, String error) {
        commandSuccess(message.getTextChannel(), author, error);
    }

    private void commandError(Message message, User author, String error) {
        commandError(message.getTextChannel(), author, error);
    }

    @Override
    public void onEvent(Event ev) {
        if (ev instanceof MessageReceivedEvent) {
            MessageReceivedEvent e = (MessageReceivedEvent) ev;

            if (e.getChannel() instanceof PrivateChannel) {
                PrivateChannel pc = (PrivateChannel) e.getChannel();
                if ("121387133821911040".equals(pc.getUser().getId()) && e.getMessage().getContentRaw().trim().equalsIgnoreCase("!restart-radio")) {
                    pc.sendMessage("Shutting down and restarting...").queue();

                    DiscordRadio.shutdown(true);
                    return;
                }
            }

            if (!e.getChannel().equals(radioChannel) && !e.getChannel().equals(djChannel_temp)) return;
            if (e.getAuthor().isBot()) return;

            User author = e.getAuthor();
            Message message = e.getMessage();
            TextChannel channel = (TextChannel) e.getChannel();

            String[] args = message.getContentRaw().split(" ");

            ////Global commands////
            if (args[0].equalsIgnoreCase("!queue")) {
                channel.sendMessage("```" + activePlaylist.getSongs().getFormattedQueue() + "```").queue();
                return;

            } else if (args[0].equalsIgnoreCase("!search") || args[0].equalsIgnoreCase("!play")) {
                if (args.length < 2) {
                    commandError(message, author, "YouTube search required");
                    return;
                }

                addNetworkTrack(author, channel, "ytsearch:" + message.getContentRaw().substring(args[0].length()).trim(), channel.equals(djChannel_temp), false, false, true);
                return;
            }

            ////DJ commands////
            if (channel.equals(djChannel_temp)) {
                if (args[0].equalsIgnoreCase("!songs")) {
                    if (args.length < 2) {
                        commandError(message, author, "Page number required");
                        return;
                    }

                    int page;
                    try {
                        page = Integer.valueOf(args[1]);
                    } catch (Exception ignored) {
                        commandError(message, author, "Invalid page number");
                        return;
                    }

                    String map = activePlaylist.getSongs().getFormattedMap(page);
                    if (map == null) {
                        commandError(message, author, "Invalid page number");
                        return;
                    }

                    channel.sendMessage("```" + map + "```").queue();
                    return;
                } else if (args[0].equalsIgnoreCase("!seek")) {
                    if (args.length < 2) {
                        commandError(message, author, "Time to seek to required (in secs)");
                        return;
                    }

                    if (!player.getPlayingTrack().isSeekable()) {
                        commandError(message, author, "This track isn't seekable");
                        return;
                    }

                    long time;
                    try {
                        time = Integer.valueOf(args[1]) * 1000;
                    } catch (Exception ignored) {
                        commandError(message, author, "Invalid number of seconds");
                        return;
                    }

                    if (time < 0) {
                        commandError(message, author, "Invalid number of seconds");
                        return;
                    }

                    player.getPlayingTrack().setPosition(time);
                    commandSuccess(message, author, "Seeked to " + FormattingUtils.getFormattedMsTime(time));
                    return;

                } else if (args[0].equalsIgnoreCase("!radio-announce")) {
                    if (args.length < 2) {
                        commandError(message, author, "Hex colour and/or announcement content required\nhttps://www.google.co.uk/search?q=colour+picker");
                        return;
                    }

                    Color colour = Color.white;

                    if (args[1].startsWith("#")) {
                        try {
                            colour = Color.decode(args[1].toUpperCase());
                            args[1] = "";
                        } catch (Exception ignored) {
                            commandError(message, author, "Hex colour format is invalid\nhttps://www.google.co.uk/search?q=colour+picker");
                            return;
                        }
                    }

                    args[0] = "";

                    MessageEmbed embed = new EmbedBuilder().setTitle("Announcement").setDescription(String.join(" ", args).trim()).setTimestamp(OffsetDateTime.now()).setColor(colour).build();

                    djChannel_temp.sendMessage(embed).queue();
                    radioChannel.sendMessage(embed).queue();
                    return;

                } else if (args[0].equalsIgnoreCase("!switch-playlist")) {
                    if (args.length < 2) {
                        commandError(message, author, "Playlist name required. Valid playlists: " + String.join(", ", playlists.stream().map(SongPlaylist::getInternal).collect(Collectors.toList())));
                        return;
                    }

                    Optional<SongPlaylist> playlist = playlists.stream().filter(p -> p.getInternal().equalsIgnoreCase(args[1])).findFirst();

                    if (!playlist.isPresent()) {
                        commandError(message, author, "Couldn't find that playlist");
                        return;
                    }

                    setActivePlaylist(playlist.get());
                    return;

                } else if (args[0].equalsIgnoreCase("!play-song")) {
                    if (args.length < 2) {
                        commandError(message, author, "Song number required. Use !songs to list songs");
                        return;
                    }

                    int song;
                    try {
                        song = Integer.valueOf(args[1]);
                    } catch (Exception ignored) {
                        commandError(message, author, "Invalid song number. Use !songs to list songs");
                        return;
                    }

                    song--;
                    List<Song> map = activePlaylist.getSongs().getSongMap();

                    if (song >= 0 && song < map.size()) {
                        playSong(map.get(song));
                        return;
                    }

                    commandError(message, author, "Invalid song number. Use !songs to list songs");
                    return;

                } else if (args[0].equalsIgnoreCase("!playlists")) {
                    channel.sendMessage("```[Playlists]\n\n" + playlists.stream().map(p -> p.getName() + (activePlaylist.equals(p) ? " (ACTIVE) [" : " [") + p.getInternal() + "]").collect(Collectors.joining("\n")) + "```").queue();
                    return;

                } else if (args[0].equalsIgnoreCase("!karaoke")) {
                    if (args.length > 1) {
                        TextChannel lyrics = jda.getTextChannelById(args[1]);

                        if (lyrics == null) {
                            commandError(message, author, "Invalid channel ID. Use the right click menu to get channel IDs");
                            return;
                        }

                        //lyricsChannel = lyrics;

                        commandSuccess(message, author, "Set lyrics channel to #" + lyrics.getName());
                    }

                    //setKaraokeMode(!karaokeMode);
                    return;

                } else if (args[0].equalsIgnoreCase("!stop-radio")) {
                    DiscordRadio.shutdown(false);
                    return;

                } else if (args[0].equalsIgnoreCase("!restart-radio")) {
                    DiscordRadio.shutdown(true);
                    return;

                } else if (args[0].equalsIgnoreCase("!shuffle")) {
                    Collections.shuffle(activePlaylist.getSongs().getQueue());
                    commandSuccess(message, author, "Shuffled all songs in the current playlist");
                    return;

                } else if (args[0].equalsIgnoreCase("!tasks")) {
                    StringBuilder built = new StringBuilder("```[Tasks]\n\n");
                    int i = 0;
                    for (RadioTaskComposition c : TaskManager.getTasks()) {
                        i++;

                        built.append(i).append(": ").append(c.getName()).append(" (").append(c.getExecutionCron()).append(")\n");
                    }

                    built.append("```");
                    channel.sendMessage(built.toString()).queue();
                    return;

                } else if (args[0].equalsIgnoreCase("!run-task")) {
                    if (args.length < 2) {
                        commandError(message, author, "Task ID required. Use !tasks to list tasks");
                        return;
                    }

                    int id;
                    try {
                        id = Integer.valueOf(args[1]);
                    } catch (Exception ignored) {
                        commandError(message, author, "Invalid task ID. Use !tasks to list tasks");
                        return;
                    }

                    if (id < 1 || id > TaskManager.getTasks().size()) {
                        commandError(message, author, "Invalid task ID. Use !tasks to list tasks");
                        return;
                    }

                    RadioTaskComposition comp = TaskManager.getTasks().get(id - 1);
                    TaskManager.executeComposition(comp, true);

                    commandSuccess(message, author, "Executed task");
                    return;

                } else if (args[0].equalsIgnoreCase("!cancel-task")) {
                    if (args.length < 2) {
                        commandError(message, author, "Task ID required. Use !tasks to list tasks");
                        return;
                    }

                    int id;
                    try {
                        id = Integer.valueOf(args[1]);
                    } catch (Exception ignored) {
                        commandError(message, author, "Invalid task ID. Use !tasks to list tasks");
                        return;
                    }

                    if (id < 1 || id > TaskManager.getTasks().size()) {
                        commandError(message, author, "Invalid task ID. Use !tasks to list tasks");
                        return;
                    }

                    RadioTaskComposition comp = TaskManager.getTasks().get(id - 1);
                    comp.setCancelled(true);

                    commandSuccess(message, author, "Cancelled task");
                    return;

                } else if (args[0].equalsIgnoreCase("!reload")) {
                    loadPlaylists();
                    radio.startTaskManager();
                    channel.sendMessage("Reloaded playlists and tasks").queue();
                    return;

                } else if (args[0].equalsIgnoreCase("!radio-commands")) {
                    channel.sendMessage("```[Commands]\n\n" +
                            "!search|!play <search ...> - shows YouTube search results\n" +
                            "!songs <page#> - lists all songs and their ID\n" +
                            "!play-song <song#> - plays the specified song\n" +
                            "!seek <pos> - seeks the current song to the specified position (in seconds)\n" +
                            "!karaoke [lyrics-chat#] - activates karaoke mode (outputs lyrics, records singing)\n" +
                            "!queue - lists the next 10 songs in the queue\n" +
                            "!playlists - lists all playlists\n" +
                            "!switch-playlist <name> - switches to a playlist (based on folder name)\n" +
                            "!shuffle - shuffles the songs in the current playlist\n" +
                            "!radio-announce [hex-colour] <message ...> - announces a message with a specified embed colour\n" +
                            "!tasks - lists all active tasks\n" +
                            "!run-task <task#> - manually runs a task\n" +
                            "!cancel-task <task#> - cancels the next execution of a task\n" +
                            "!reload - reloads playlists and tasks - only do this after creating/deleting playlists or tasks\n" +
                            "!stop-radio - shuts down the bot - only do this if something breaks\n" +
                            "!restart-radio - restarts the bot```").queue();
                    return;
                }
            }

            addNetworkTrack(author, channel, message.getContentRaw(), channel.equals(djChannel_temp), false, false, false);
        } else if (ev instanceof MessageReactionAddEvent) {
            MessageReactionAddEvent e = (MessageReactionAddEvent) ev;
            if (e.getUser().isBot()) return;

            if (searchPlaylists.containsKey(e.getMessageIdLong())) {
                if (searchPlaylists.get(e.getMessageIdLong()).handleReaction(e)) {
                    searchPlaylists.remove(e.getMessageIdLong());
                }
            }
        }
    }

    public boolean areSuggestionsEnabled() {
        return suggestionsEnabled;
    }

    public boolean setSuggestionsEnabled(boolean enabled, User source) {
        songEventListeners.forEach(l -> l.onSuggestionsToggle(enabled, source));

        return suggestionsEnabled = enabled;
    }

    public SongQueue getSpecialQueue() {
        return specialQueue;
    }

    public DiscordRadio getRadio() {
        return radio;
    }

    public List<Song> getAwaitingSpecialSongs() {
        return awaitingSpecialSongs;
    }

    public void addSearchMessage(SongSearchPlaylist playlist, Message msg) {
        searchPlaylists.put(msg.getIdLong(), playlist);
    }

    public Map<Song, String> getNowPlayingSongOverrides() {
        return nowPlayingSongOverrides;
    }
}