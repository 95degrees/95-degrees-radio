package me.voidinvoid.discordmusic;

import com.github.natanbc.lavadsp.timescale.TimescalePcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.lava.extensions.youtuberotator.YoutubeIpRotatorSetup;
import com.sedmelluq.lava.extensions.youtuberotator.planner.NanoIpRoutePlanner;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv6Block;
import me.voidinvoid.discordmusic.audio.AudioPlayerSendHandler;
import me.voidinvoid.discordmusic.cache.YouTubeCacheManager;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.events.NetworkSongError;
import me.voidinvoid.discordmusic.events.NetworkSongException;
import me.voidinvoid.discordmusic.events.RadioEventListener;
import me.voidinvoid.discordmusic.levelling.Achievement;
import me.voidinvoid.discordmusic.levelling.AchievementManager;
import me.voidinvoid.discordmusic.sfx.SoundEffectsManager;
import me.voidinvoid.discordmusic.songs.*;
import me.voidinvoid.discordmusic.songs.database.DatabaseRadioPlaylist;
import me.voidinvoid.discordmusic.songs.database.DatabaseSong;
import me.voidinvoid.discordmusic.songs.local.LocalSongQueue;
import me.voidinvoid.discordmusic.spotify.SpotifyManager;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.ConsoleColor;
import me.voidinvoid.discordmusic.utils.Service;
import me.voidinvoid.discordmusic.utils.Songs;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SongOrchestrator extends AudioEventAdapter implements RadioService {

    public static long MAX_SONG_LENGTH = 8 * 60 * 1000; //8 mins
    public static int MAX_CONCURRENT_SUGGESTIONS = 5; //can queue 5 songs at once

    private List<Playlist> playlists;
    private Playlist activePlaylist;
    private int timeUntilJingle = 0;
    private DefaultAudioPlayerManager manager;
    private AudioPlayer player;
    private AudioPlayerSendHandler audioSendHandler;
    private TimescalePcmAudioFilter timescaleFilter;
    private float timescale = 1.0f;
    private float pitch = 1.0f;

    private boolean pausePending;

    private JDA jda;
    private boolean suggestionsEnabled = true;
    private boolean queueCommandEnabled = true;
    private SongQueue specialQueue;

    private Radio radio;

    private List<Song> awaitingSpecialSongs = new ArrayList<>();

    private List<RadioEventListener> radioEventListeners = new ArrayList<>();

    public SongOrchestrator(Radio radio) {

        this.radio = radio;
        jda = radio.getJda();

        manager = new DefaultAudioPlayerManager();
        manager.getConfiguration().setFilterHotSwapEnabled(true);

        AudioSourceManagers.registerLocalSource(manager);
        AudioSourceManagers.registerRemoteSources(manager);

        var youtube = manager.source(YoutubeAudioSourceManager.class);
        new YoutubeIpRotatorSetup(new NanoIpRoutePlanner(Collections.singletonList(new Ipv6Block("2001:bc8:1824:1201::/64")), true))
                .forSource(youtube).setup();

        player = manager.createPlayer();
        player.setFilterFactory((track, format, filter) -> {
            timescaleFilter = new TimescalePcmAudioFilter(filter, format.channelCount, format.sampleRate);
            timescaleFilter.setSpeed(timescale).setPitch(pitch);
            return List.of(timescaleFilter);
        });
        player.addListener(this);

        audioSendHandler = new AudioPlayerSendHandler(player);
    }

    public void registerSongEventListener(RadioEventListener listener) {
        radioEventListeners.add(listener);
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

    public List<Playlist> getPlaylists() {
        return playlists;
    }

    public int getTimeUntilJingle() {
        return timeUntilJingle;
    }

    public void setTimeUntilJingle(int timeUntilJingle) {
        this.timeUntilJingle = timeUntilJingle;
    }

    public Playlist getActivePlaylist() {
        return activePlaylist;
    }

    public void setActivePlaylist(Playlist activePlaylist) {
        if (this.activePlaylist instanceof RadioPlaylist) {
            RadioPlaylist sp = (RadioPlaylist) this.activePlaylist;
            List<Song> networkSongs = sp.getSongs().clearSuggestions();

            if (activePlaylist instanceof RadioPlaylist) {
                ((RadioPlaylist) activePlaylist).getSongs().addSuggestions(networkSongs); //transfer network queue songs across
            }
        }

        activePlaylist.onDeactivate();

        radioEventListeners.forEach(l -> {
            try {
                l.onPlaylistChange(this.activePlaylist, activePlaylist);
            } catch (Exception ex) {
                warn(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
                ex.printStackTrace();
            }
        });

        this.activePlaylist = activePlaylist;

        activePlaylist.onActivate();
    }

    public AudioPlayer getPlayer() {
        return player;
    }

    public void setTimescale(float timescale) {
        this.timescale = timescale;
        if (timescaleFilter != null) {
            timescaleFilter.setSpeed(timescale);
        }
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
        if (timescaleFilter != null) {
            timescaleFilter.setPitch(pitch);
        }
    }

    public Song getCurrentSong() {
        return player.getPlayingTrack() == null ? null : player.getPlayingTrack().getUserData(Song.class);
    }

    public void loadPlaylists() {
        log("Loading special queue...");
        specialQueue = new LocalSongQueue(Paths.get(RadioConfig.config.locations.specialPlaylist), null, SongType.SPECIAL, false);
        specialQueue.loadSongsAsync();

        playlists = new ArrayList<>();

        Playlist prevActive = activePlaylist;

        DatabaseManager db = Radio.getInstance().getService(DatabaseManager.class);
        if (db != null) {
            log("Loading database playlists...");
            ((Iterable<Document>) db.getCollection("playlists").find()).forEach(d -> playlists.add(new DatabaseRadioPlaylist(d)));
        }

        playlists.forEach(p -> {
            if (p.isDefault()) activePlaylist = p;
        });

        if (playlists.size() == 0) {
            warn(ConsoleColor.RED + "No playlists found!" + ConsoleColor.RESET);
            return;
        } else {
            log("Found " + playlists.size() + " playlists: " + playlists.stream().map(Playlist::getName).collect(Collectors.joining(", ")));
        }

        if (activePlaylist == null) {
            activePlaylist = playlists.get(0);
            warn("No default playlist found. Defaulted to " + activePlaylist.getName());
        }

        if (prevActive != null) { //keep using the same playlist we had previously
            activePlaylist = playlists.stream().filter(p -> p.getInternal().equals(prevActive.getInternal())).findAny().orElse(activePlaylist);
        }

        activePlaylist.awaitLoad();
        activePlaylist.onActivate();
    }

    public void playNextSong() {
        playNextSong(false, true, false);
    }

    public void playNextSong(boolean skipJingle, boolean decrementJingleCounter, boolean playSkipSfx) {
        if (activePlaylist == null) return;

        if (playSkipSfx) {
            player.playTrack(Service.of(SoundEffectsManager.class).SWOOSH_SOUND_EFFECT.makeClone());
            return;
        }

        if (pausePending) {
            pausePending = false;
            radioEventListeners.forEach(l -> {
                try {
                    l.onPausePending(false);
                } catch (Exception ex) {
                    log(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
                    ex.printStackTrace();
                }
            });

            player.stopTrack();
            radioEventListeners.forEach(l -> {
                try {
                    l.onTrackStopped();
                } catch (Exception ex) {
                    log(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
                    ex.printStackTrace();
                }
            });
            return;
        }

        if (decrementJingleCounter) timeUntilJingle--;
        boolean jingle = !skipJingle && timeUntilJingle < 0;

        if (awaitingSpecialSongs.size() > 0) {
            playSong(awaitingSpecialSongs.remove(0));
            return;
        }

        if (jingle) {
            timeUntilJingle = RadioConfig.config.orchestration.jingleFrequency;
            playSong(activePlaylist.provideNextSong(true));
            return;
        }

        playSong(activePlaylist.provideNextSong(false));
    }

    public void playSong(final Song song) {
        if (song == null) {
            radioEventListeners.forEach(l -> {
                try {
                    l.onNoSongsInQueue(activePlaylist);
                } catch (Exception ex) {
                    log(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
                    ex.printStackTrace();
                }
            });
            return;
        }

        log(ConsoleColor.PURPLE_BACKGROUND_BRIGHT + " Now Playing " + ConsoleColor.RESET_SPACE + ConsoleColor.WHITE_BOLD + Songs.titleArtist(song) + ConsoleColor.RESET);
        log("              Jingle after " + timeUntilJingle + " more songs");

        String cacheFileName = null;

        if ((song instanceof DatabaseSong || song instanceof SpotifySong) && (song.getLavaIdentifier().toLowerCase().contains("youtu.be") || song.getLavaIdentifier().toLowerCase().contains("youtube.com"))) { //fetch cached version if available
            cacheFileName = Service.of(YouTubeCacheManager.class).loadOrCache(song.getLavaIdentifier());
        }

        if (cacheFileName != null && song.getTrack() != null) {
            player.playTrack(song.getTrack());
            return;
        }

        if (cacheFileName != null) {
            log("Loading this song from cache");
        }

        manager.loadItem(cacheFileName != null ? cacheFileName : song.getLavaIdentifier(), new AudioLoadResultHandler() {
            public void trackLoaded(AudioTrack track) {
                track.setUserData(song);
                player.playTrack(track);
            }

            public void playlistLoaded(AudioPlaylist p) {
            }

            public void noMatches() {
                log("No match found for file");
                playNextSong(false, false, false);
            }

            public void loadFailed(FriendlyException e) {
                log("Load failed for " + song.getLavaIdentifier() + ": " + e.getMessage());
                playNextSong(false, false, false);

                radioEventListeners.forEach(l -> {
                    try {
                        l.onSongLoadError(song, e);
                    } catch (Exception ex) {
                        log(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
                        ex.printStackTrace();
                    }
                });
            }
        });
    }

    public void seekTrack(long seekTime) {
        AudioTrack track = player.getPlayingTrack();

        if (track == null) return;

        final long pos = Math.max(0, Math.min(seekTime, track.getDuration())); //normalise

        if (!track.isSeekable()) return;
        track.setPosition(pos);

        radioEventListeners.forEach(l -> {
            try {
                l.onSongSeek(track, pos, player);
            } catch (Exception ex) {
                log(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
                ex.printStackTrace();
            }
        });
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        final Song song = track.getUserData(Song.class);

        if (song == null) {
            return;
        }

        radioEventListeners.forEach(l -> {
            try {
                l.onSongStart(song, track, player, timeUntilJingle);
            } catch (Exception ex) {
                log(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
                ex.printStackTrace();
            }
        });
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext || endReason == AudioTrackEndReason.REPLACED) {
            Song song = track.getUserData(Song.class);

            if (song != null) {
                radioEventListeners.forEach(l -> {
                    try {
                        l.onSongEnd(song, track);
                    } catch (Exception ex) {
                        log(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
                        ex.printStackTrace();
                    }
                });
            }
        }

        if (endReason.mayStartNext) {
            playNextSong();
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        playNextSong(false, false, false);
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        setTimeUntilJingle(-1);
        playNextSong();
    }

    public CompletableFuture<NetworkSong> createNetworkSong(SongType type, String url) {
        return createTrack(url).thenApply(t -> t == null ? null : new NetworkSong(type, t, null, null));
    }

    public CompletableFuture<AudioTrack> createTrack(String identifier) {
        var future = new CompletableFuture<AudioTrack>();

        manager.loadItem(identifier, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                future.complete(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                future.complete(null);
            }

            @Override
            public void noMatches() {
                future.complete(null);
            }

            @Override
            public void loadFailed(FriendlyException e) {
                future.complete(null);
            }
        });

        return future;
    }

    public <T extends Song> CompletableFuture<T> queueSuggestion(T song, QueueOption... options) { //todo dynamically load audiotrack here?
        var opts = Arrays.asList(options);
        var future = new CompletableFuture<T>();

        log("Adding suggested song '" + song.getTitle() + "' to queue");

        final Member member;

        if (song instanceof UserSuggestable) {
            var s = ((UserSuggestable) song);
            var mb = s.getSuggestedBy();

            if (mb != null) {
                member = Radio.getInstance().getGuild().retrieveMember(mb).complete();
            } else {
                member = null;
            }
        } else {
            member = null;
        }

        if (!(activePlaylist instanceof RadioPlaylist)) {
            future.completeExceptionally(new NetworkSongException(NetworkSongError.INVALID_PLAYLIST_TYPE));
            return future;
        }

        if (!opts.contains(QueueOption.BYPASS_ERRORS) && song instanceof NetworkSong && song.getTrack() instanceof LocalAudioTrack) {
            log(member + " tried to play a local track - disallowed");
            future.completeExceptionally(new NetworkSongException(NetworkSongError.ILLEGAL_SONG_LOCATION));
            return future;
        }

        RadioPlaylist sp = (RadioPlaylist) activePlaylist;

        if (!opts.contains(QueueOption.BYPASS_ERRORS)) {
            final NetworkSongError error;

            if (!suggestionsEnabled) {
                error = NetworkSongError.SONG_SUGGESTIONS_DISABLED;
            } else if (sp.getSongs().suggestionsBy(member == null ? null : member.getUser()).size() >= MAX_CONCURRENT_SUGGESTIONS) {
                error = NetworkSongError.QUEUE_LIMIT_REACHED;
            } else if (song.getTrack().getInfo().isStream) {
                error = NetworkSongError.IS_STREAM;
            } else if (song.getTrack().getDuration() > MAX_SONG_LENGTH) {
                if (member != null && song.getTrack().getDuration() - MAX_SONG_LENGTH <= 1000) {
                    Radio.getInstance().getService(AchievementManager.class).rewardAchievement(member.getUser(), Achievement.OVER_LENGTH_LIMIT);
                }
                error = NetworkSongError.EXCEEDS_LENGTH_LIMIT;
            } else if (member != null && (member.getVoiceState() == null || !member.getVoiceState().inVoiceChannel() || !ChannelScope.RADIO_VOICE.check(member.getVoiceState().getChannel()))) {
                error = NetworkSongError.NOT_IN_VOICE_CHANNEL;
            } else {
                error = null;
            }

            log(error);

            if (error != null) {
                radioEventListeners.forEach(l -> {
                    try {
                        l.onSongQueueError(song, song.getTrack(), member, error);
                    } catch (Exception ex) {
                        log(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
                        ex.printStackTrace();
                    }
                });

                future.completeExceptionally(new NetworkSongException(error));
                return future;
            }
        }

        final int index;

        if (opts.contains(QueueOption.PUSH_TO_START) || opts.contains(QueueOption.PLAY_INSTANTLY)) {
            index = 0;
            sp.getSongs().getQueue().add(0, song);
        } else {
            index = sp.getSongs().addSuggestion(song);
        }

        if (song instanceof SpotifyTrackHolder && ((SpotifyTrackHolder) song).getSpotifyTrack() == null && song.getType() == SongType.SONG) {
            Service.of(SpotifyManager.class).searchTrack(song.getTitle()).whenComplete((spotify, ex) -> {
                if (spotify != null) {
                    ((SpotifyTrackHolder) song).setSpotifyTrack(spotify);
                }

                future.complete(song);
            });
        } else {
            future.complete(song);
        }

        future.thenRun(() -> {
            radioEventListeners.forEach(l -> {
                try {
                    l.onSongQueued(song, song.getTrack(), member, index);
                } catch (Exception ex) {
                    log(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
                    ex.printStackTrace();
                }
            });

            if (opts.contains(QueueOption.PLAY_INSTANTLY)) {
                player.setPaused(false);
                player.stopTrack();
                playNextSong(true, false, false);
            }

            log("Network track is in the queue: #" + (index + 1));
        });

        return future;
    }

    public boolean areSuggestionsEnabled() {
        return suggestionsEnabled;
    }

    public boolean setSuggestionsEnabled(boolean enabled, User source) {
        radioEventListeners.forEach(l -> {
            try {
                l.onSuggestionsToggle(enabled, source);
            } catch (Exception ex) {
                log(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
                ex.printStackTrace();
            }
        });

        return suggestionsEnabled = enabled;
    }

    public void setPaused(boolean paused) {
        boolean wasPaused = player.isPaused();

        player.setPaused(paused);

        final boolean p = player.isPaused(); //since setPaused might not have an effect (e.g. for streams)
        if (wasPaused == p) return;

        final AudioTrack t = player.getPlayingTrack();
        final Song s = t.getUserData(Song.class);

        radioEventListeners.forEach(l -> {
            try {
                l.onSongPause(p, s, t, player);
            } catch (Exception ex) {
                log(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
                ex.printStackTrace();
            }
        });
    }

    public SongQueue getSpecialQueue() {
        return specialQueue;
    }

    public Radio getRadio() {
        return radio;
    }

    public List<Song> getAwaitingSpecialSongs() {
        return awaitingSpecialSongs;
    }

    public boolean isQueueCommandEnabled() {
        return queueCommandEnabled;
    }

    public void setQueueCommandEnabled(boolean queueCommandEnabled) {
        this.queueCommandEnabled = queueCommandEnabled;
    }

    public boolean isPausePending() {
        return pausePending;
    }

    public void setPausePending(boolean pausePending) {
        if (pausePending == this.pausePending) return;
        this.pausePending = pausePending;

        radioEventListeners.forEach(l -> {
            try {
                l.onPausePending(pausePending);
            } catch (Exception ex) {
                log(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
                ex.printStackTrace();
            }
        });
    }
}