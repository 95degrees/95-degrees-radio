package me.voidinvoid.discordmusic;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioTrack;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import me.voidinvoid.discordmusic.audio.AudioPlayerSendHandler;
import me.voidinvoid.discordmusic.cache.YouTubeCacheManager;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.events.NetworkSongError;
import me.voidinvoid.discordmusic.events.NetworkSongException;
import me.voidinvoid.discordmusic.events.SongEventListener;
import me.voidinvoid.discordmusic.levelling.*;
import me.voidinvoid.discordmusic.songs.*;
import me.voidinvoid.discordmusic.songs.database.DatabaseRadioPlaylist;
import me.voidinvoid.discordmusic.songs.database.DatabaseSong;
import me.voidinvoid.discordmusic.songs.local.LocalRadioPlaylist;
import me.voidinvoid.discordmusic.songs.local.LocalSongQueue;
import me.voidinvoid.discordmusic.spotify.SpotifyManager;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.ConsoleColor;
import me.voidinvoid.discordmusic.utils.Service;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SongOrchestrator extends AudioEventAdapter implements RadioService {

    private List<Playlist> playlists;
    private Playlist activePlaylist;
    private int timeUntilJingle = 0;
    private DefaultAudioPlayerManager manager;
    private AudioPlayer player;
    private AudioPlayerSendHandler audioSendHandler;

    private boolean pausePending;

    private JDA jda;
    private boolean suggestionsEnabled = true;
    private boolean queueCommandEnabled = true;
    private SongQueue specialQueue;

    private Radio radio;

    private List<Song> awaitingSpecialSongs = new ArrayList<>();

    private List<SongEventListener> songEventListeners = new ArrayList<>();

    public SongOrchestrator(Radio radio, RadioConfig config) {

        this.radio = radio;
        jda = radio.getJda();

        loadPlaylists();

        manager = new DefaultAudioPlayerManager();

        AudioSourceManagers.registerLocalSource(manager);
        AudioSourceManagers.registerRemoteSources(manager);

        player = manager.createPlayer();
        player.addListener(this);

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
            List<NetworkSong> networkSongs = sp.getSongs().clearNetworkSongs();

            if (activePlaylist instanceof RadioPlaylist) {
                ((RadioPlaylist) activePlaylist).getSongs().addNetworkSongs(networkSongs); //transfer network queue songs across
            }
        }

        activePlaylist.onDeactivate();

        songEventListeners.forEach(l -> {
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
            log(ConsoleColor.CYAN_BACKGROUND_BRIGHT + ConsoleColor.BLACK_BRIGHT + " Playlist " + ConsoleColor.RESET_SPACE + p.getName());
            if (p.isDefault()) activePlaylist = p;
        });

        if (playlists.size() == 0) {
            warn(ConsoleColor.RED + "No playlists found!" + ConsoleColor.RESET);
            return;
        }

        if (activePlaylist == null) {
            activePlaylist = playlists.get(0);
            warn("No default song directory found. Defaulted to " + activePlaylist.getName());
        }

        if (prevActive != null) { //keep using the same playlist we had previously
            activePlaylist = playlists.stream().filter(p -> p.getInternal().equals(prevActive.getInternal())).findAny().orElse(activePlaylist);
        }

        activePlaylist.awaitLoad();
        activePlaylist.onActivate();
    }

    public void playNextSong() {
        playNextSong(false, true);
    }

    public void playNextSong(boolean skipJingle, boolean decrementJingleCounter) {
        if (activePlaylist == null) return;

        if (pausePending) {
            pausePending = false;
            songEventListeners.forEach(l -> {
                try {
                    l.onPausePending(false);
                } catch (Exception ex) {
                    log(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
                    ex.printStackTrace();
                }
            });

            player.stopTrack();
            songEventListeners.forEach(l -> {
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
            songEventListeners.forEach(l -> {
                try {
                    l.onNoSongsInQueue(activePlaylist);
                } catch (Exception ex) {
                    log(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
                    ex.printStackTrace();
                }
            });
            return;
        }

        log(ConsoleColor.BLACK_BACKGROUND_BRIGHT + " NOW PLAYING " + ConsoleColor.RESET_SPACE + ConsoleColor.WHITE_BOLD + song.getFriendlyName() + ConsoleColor.RESET);
        log("              Jingle after " + timeUntilJingle + " more songs");

        String cacheFileName = null;

        if (song instanceof DatabaseSong && (song.getFullLocation().toLowerCase().contains("youtu.be") || song.getFullLocation().toLowerCase().contains("youtube.com"))) { //fetch cached version if available
            cacheFileName = Service.of(YouTubeCacheManager.class).loadOrCache(song.getFullLocation());
        }

        if (cacheFileName != null && song.getTrack() != null) {
            player.playTrack(song.getTrack());
            return;
        }

        if (cacheFileName != null) {
            log("Loading this song from cache");
        }

        manager.loadItem(cacheFileName != null ? cacheFileName : song.getFullLocation(), new AudioLoadResultHandler() {
            public void trackLoaded(AudioTrack track) {
                track.setUserData(song);
                player.playTrack(track);
            }

            public void playlistLoaded(AudioPlaylist p) {
            }

            public void noMatches() {
                log("No match found for file");
                playNextSong(false, false);
            }

            public void loadFailed(FriendlyException e) {
                log("Load failed for file: (ID) " + song.getFullLocation());
                e.printStackTrace();
                playNextSong(false, false);

                songEventListeners.forEach(l -> {
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

        songEventListeners.forEach(l -> {
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

        songEventListeners.forEach(l -> {
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
            songEventListeners.forEach(l -> {
                try {
                    l.onSongEnd(song, track);
                } catch (Exception ex) {
                    log(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
                    ex.printStackTrace();
                }
            });
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

    public void createNetworkTrack(SongType type, String url, Consumer<NetworkSong> callback) {
        manager.loadItem(url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                callback.accept(new NetworkSong(type, audioTrack, null));
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                callback.accept(null);
            }

            @Override
            public void noMatches() {
                callback.accept(null);
            }

            @Override
            public void loadFailed(FriendlyException e) {
                callback.accept(null);
            }
        });
    }

    public boolean addNetworkTrack(Member suggestedBy, AudioTrack track, boolean bypassErrors, boolean playInstantly, boolean pushToStart) {
        return addNetworkTrack(suggestedBy, track, bypassErrors, playInstantly, pushToStart, null, null);
    }

    public boolean addNetworkTrack(Member suggestedBy, AudioTrack track, boolean bypassErrors, boolean playInstantly, boolean pushToStart, Consumer<NetworkSong> successCallback, Consumer<NetworkSongException> errorCallback) {
        log("Found network track '" + track.getInfo().title + "', suggested by " + suggestedBy);

        User user = suggestedBy == null ? null : suggestedBy.getUser();

        if (!bypassErrors && track instanceof LocalAudioTrack) {
            log(suggestedBy + " tried to play a local track - disallowed");
            if (errorCallback != null) errorCallback.accept(new NetworkSongException(NetworkSongError.ILLEGAL_SONG_LOCATION));
            return false;
        }

        if (!(activePlaylist instanceof RadioPlaylist)) {
            if (errorCallback != null) errorCallback.accept(new NetworkSongException(NetworkSongError.INVALID_PLAYLIST_TYPE));
            return false;
        }

        RadioPlaylist sp = (RadioPlaylist) activePlaylist;

        NetworkSong song = new NetworkSong(SongType.SONG, track, user);

        if (!bypassErrors) {
            final NetworkSongError error;

            long maxLength = RadioConfig.config.orchestration.maxSongLength;
            int maxSuggestions = RadioConfig.config.orchestration.userQueueLimit;

            LevellingManager lm = Radio.getInstance().getService(LevellingManager.class);
            if (lm != null && suggestedBy != null) {
                AppliedLevelExtra a = lm.getLatestExtra(suggestedBy.getUser(), LevelExtras.MAX_SUGGESTION_LENGTH);
                if (a != null) maxLength = (long) a.getValue();

                a = lm.getLatestExtra(suggestedBy.getUser(), LevelExtras.MAX_SUGGESTIONS_IN_QUEUE);
                if (a != null) maxSuggestions = (int) a.getValue();
            }

            if (!suggestionsEnabled) {
                error = NetworkSongError.SONG_SUGGESTIONS_DISABLED;
            } else if (sp.getSongs().suggestionsBy(user).size() >= maxSuggestions) {
                error = NetworkSongError.QUEUE_LIMIT_REACHED;
            } else if (track.getInfo().isStream) {
                error = NetworkSongError.IS_STREAM;
            } else if (track.getDuration() > maxLength) {
                if (suggestedBy != null && track.getDuration() - maxLength <= 1000) {
                    Radio.getInstance().getService(AchievementManager.class).rewardAchievement(suggestedBy.getUser(), Achievement.OVER_LENGTH_LIMIT);
                }
                error = NetworkSongError.EXCEEDS_LENGTH_LIMIT;
            } else if (suggestedBy != null && (!suggestedBy.getVoiceState().inVoiceChannel() || !ChannelScope.RADIO_VOICE.check(suggestedBy.getVoiceState().getChannel()))) {
                error = NetworkSongError.NOT_IN_VOICE_CHANNEL;
            } else {
                error = null;
            }

            if (error != null) {
                songEventListeners.forEach(l -> {
                    try {
                        l.onNetworkSongQueueError(song, track, suggestedBy, error);
                    } catch (Exception ex) {
                        log(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
                        ex.printStackTrace();
                    }
                });
                if (errorCallback != null) errorCallback.accept(new NetworkSongException(error));
                return false;
            }
        }

        final int index;

        if (pushToStart || playInstantly) {
            index = 0;
        } else {
            index = sp.getSongs().addNetworkSong(song);
        }

        if (successCallback != null) {
            successCallback.accept(song);
        }

        songEventListeners.forEach(l -> {
            try {
                l.onNetworkSongQueued(song, track, suggestedBy, index);
            } catch (Exception ex) {
                log(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
                ex.printStackTrace();
            }
        });

        if (pushToStart || playInstantly) {
            sp.getSongs().getQueue().add(0, song);

            if (playInstantly) {
                player.setPaused(false);
                player.stopTrack();
                playNextSong(true, false);
            }
        }

        log("Network track is in the queue: #" + (index + 1));

        return true;
    }

    public boolean areSuggestionsEnabled() {
        return suggestionsEnabled;
    }

    public boolean setSuggestionsEnabled(boolean enabled, User source) {
        songEventListeners.forEach(l -> {
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

        songEventListeners.forEach(l -> {
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

        songEventListeners.forEach(l -> {
            try {
                l.onPausePending(pausePending);
            } catch (Exception ex) {
                log(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
                ex.printStackTrace();
            }
        });
    }
}