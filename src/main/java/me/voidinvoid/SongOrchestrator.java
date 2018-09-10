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

        if (!skipJingle && (timeUntilJingle < 0)) {
            timeUntilJingle = JINGLE_FREQUENCY;
            playSong(activePlaylist.getJingles().getRandom());
            return;
        }

        if (awaitingSpecialSongs.size() > 0) {
            playSong(awaitingSpecialSongs.remove(0));
            return;
        }

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