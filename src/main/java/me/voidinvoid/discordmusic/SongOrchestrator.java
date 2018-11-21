package me.voidinvoid.discordmusic;

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
import me.voidinvoid.discordmusic.audio.AudioPlayerSendHandler;
import me.voidinvoid.discordmusic.audio.twitch.TwitchKrakenStreamAudioSourceManager;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.events.NetworkSongError;
import me.voidinvoid.discordmusic.events.SongEventListener;
import me.voidinvoid.discordmusic.songs.*;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.ConsoleColor;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SongOrchestrator extends AudioEventAdapter {

    public static final int JINGLE_FREQUENCY = 3;
    public static final int MAX_SONG_LENGTH = 300000;
    public static final int USER_QUEUE_LIMIT = 3;

    private List<Playlist> playlists;
    private Playlist activePlaylist;
    private int timeUntilJingle = 0;
    private DefaultAudioPlayerManager manager;
    private AudioPlayer player;
    private AudioPlayerSendHandler audioSendHandler;

    private boolean pausePending;

    private JDA jda;
    private Path playlistsRoot;
    private boolean suggestionsEnabled = true;
    private boolean queueCommandEnabled = true;
    private SongQueue specialQueue;

    private Radio radio;

    private List<Song> awaitingSpecialSongs = new ArrayList<>();

    private List<SongEventListener> songEventListeners = new ArrayList<>();

    public SongOrchestrator(Radio radio, RadioConfig config) {

        this.radio = radio;
        jda = radio.getJda();

        this.playlistsRoot = Paths.get(config.locations.playlists);

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
        if (this.activePlaylist instanceof SongPlaylist) {
            SongPlaylist sp = (SongPlaylist) this.activePlaylist;
            List<NetworkSong> networkSongs = sp.getSongs().clearNetworkSongs();

            if (activePlaylist instanceof SongPlaylist) {
                ((SongPlaylist) activePlaylist).getSongs().addNetworkSongs(networkSongs); //transfer network queue songs across
            }
        }

        songEventListeners.forEach(l -> {
            try {
                l.onPlaylistChange(this.activePlaylist, activePlaylist);
            } catch (Exception ex) {
                System.out.println(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
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
        return player.getPlayingTrack().getUserData(Song.class);
    }

    public void loadPlaylists() {
        System.out.println("Loading special queue...");
        specialQueue = new SongQueue(null, Paths.get(RadioConfig.config.locations.specialPlaylist), SongType.SPECIAL, false);
        specialQueue.loadSongsAsync();

        System.out.println("Loading playlists...");

        Playlist prevActive = activePlaylist;

        try (Stream<Path> playlistFolder = Files.list(playlistsRoot)) {
            playlists = playlistFolder
                    .filter(Files::isDirectory)
                    .map(SongPlaylist::new)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.out.println(ConsoleColor.RED + "IO error scanning playlists directory" + ConsoleColor.RESET);
            e.printStackTrace();
            return;
        }


        playlists.forEach(p -> {
            System.out.println(ConsoleColor.CYAN_BACKGROUND_BRIGHT + ConsoleColor.BLACK_BRIGHT + " PLAYLIST " + ConsoleColor.RESET_SPACE + p.getName());

            if (p.isDefault()) activePlaylist = p;
        });

        if (playlists.size() == 0) {
            System.out.println(ConsoleColor.RED + "No playlists found!" + ConsoleColor.RESET);
            return;
        }

        if (activePlaylist == null) {
            activePlaylist = playlists.get(0);
            System.out.println(ConsoleColor.YELLOW + "Warning: no default song directory found. Defaulted to " + activePlaylist.getName() + ConsoleColor.RESET);
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

            player.stopTrack();
            songEventListeners.forEach(l -> {
                try {
                    l.onTrackStopped();
                } catch (Exception ex) {
                    System.out.println(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
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
            timeUntilJingle = JINGLE_FREQUENCY;
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
                    System.out.println(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
                    ex.printStackTrace();
                }
            });
            return;
        }

        System.out.println(ConsoleColor.BLACK_BACKGROUND_BRIGHT + " NOW PLAYING " + ConsoleColor.RESET_SPACE + ConsoleColor.WHITE_BOLD + song.getFileName() + ConsoleColor.RESET);
        System.out.println("              Jingle after " + timeUntilJingle + " more songs");

        if (song.getTrack() != null) {
            player.playTrack(song.getTrack());
            return;
        }

        manager.loadItem(song.getFullLocation(), new AudioLoadResultHandler() {
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
                System.out.println("Load failed for file: (ID) " + song.getFullLocation());
                e.printStackTrace();
                playNextSong(false, false);

                songEventListeners.forEach(l -> {
                    try {
                        l.onSongLoadError(song, e);
                    } catch (Exception ex) {
                        System.out.println(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
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
                System.out.println(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
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
                System.out.println(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
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
                    System.out.println(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
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

    public boolean addNetworkTrack(Member suggestedBy, AudioTrack track, boolean bypassErrors, boolean playInstantly, boolean pushToStart) {
        System.out.println("Found network track '" + track.getInfo().title + "', suggested by " + suggestedBy);

        User user = suggestedBy == null ? null : suggestedBy.getUser();

        if (!bypassErrors && track instanceof LocalAudioTrack) {
            System.out.println(suggestedBy + " tried to play a local track - disallowed");
            return false;
        }

        if (!(activePlaylist instanceof SongPlaylist)) return false;

        SongPlaylist sp = (SongPlaylist) activePlaylist;

        NetworkSong song = new NetworkSong(SongType.SONG, track, user);

        if (!bypassErrors) {
            final NetworkSongError error;

            if (!suggestionsEnabled) {
                error = NetworkSongError.SONG_SUGGESTIONS_DISABLED;
            } else if (sp.getSongs().suggestionsBy(user).size() >= USER_QUEUE_LIMIT) {
                error = NetworkSongError.QUEUE_LIMIT_REACHED;
            } else if (track.getInfo().isStream) {
                error = NetworkSongError.IS_STREAM;
            } else if (track.getDuration() > MAX_SONG_LENGTH) {
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
                        System.out.println(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
                        ex.printStackTrace();
                    }
                });
                return false;
            }
        }

        final int index;

        if (pushToStart || playInstantly) {
            index = 0;
        } else {
            index = sp.getSongs().addNetworkSong(song);
        }

        songEventListeners.forEach(l -> {
            try {
                l.onNetworkSongQueued(song, track, suggestedBy, index);
            } catch (Exception ex) {
                System.out.println(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
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

        System.out.println("Network track is in the queue: #" + (index + 1));
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
                System.out.println(ConsoleColor.RED + "Exception in song event listener: " + ex.getMessage() + ConsoleColor.RESET);
                ex.printStackTrace();
            }
        });

        return suggestionsEnabled = enabled;
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
        this.pausePending = pausePending;
    }
}