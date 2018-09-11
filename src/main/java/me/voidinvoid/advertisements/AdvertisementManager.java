package me.voidinvoid.advertisements;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.Radio;
import me.voidinvoid.config.RadioConfig;
import me.voidinvoid.events.SongEventListener;
import me.voidinvoid.songs.Song;
import me.voidinvoid.songs.SongQueue;
import me.voidinvoid.songs.SongType;
import me.voidinvoid.utils.ConsoleColor;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.TextChannel;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;

public class AdvertisementManager implements SongEventListener {

    private static final String AD_LOG_PREFIX = ConsoleColor.CYAN_BACKGROUND + " AD " + ConsoleColor.RESET_SPACE;

    private SongQueue advertQueue;
    private List<Advertisement> adverts;

    private TextChannel textChannel;

    public AdvertisementManager(JDA jda) {
        textChannel = jda.getTextChannelById(RadioConfig.config.channels.radioChat);

        reload();
    }

    public void reload() {
        advertQueue = new SongQueue(Paths.get(RadioConfig.config.locations.advertPlaylist), SongType.ADVERTISEMENT, false);
        advertQueue.loadSongsAsync();

        try {
            adverts = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().fromJson(new String(Files.readAllBytes(Paths.get(RadioConfig.config.locations.adverts))), new TypeToken<List<Advertisement>>() {
            }.getType());
        } catch (Exception e) {
            System.out.println(AD_LOG_PREFIX + "Error loading adverts");
            e.printStackTrace();
        }
    }

    public void pushAdvertisement() {
        if (advertQueue.getQueue().size() == 0) return;

        List<Song> awaitingSongs = Radio.instance.getOrchestrator().getAwaitingSpecialSongs();

        if (awaitingSongs.stream().noneMatch(s -> s.getType() == SongType.ADVERTISEMENT)) { //only queue one ad at a time
            awaitingSongs.add(advertQueue.getNextAndMoveToEnd());
        }
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        if (song.getType() == SongType.ADVERTISEMENT) {
            Advertisement ad = adverts.stream().filter(a -> a.getFileName().equalsIgnoreCase(song.getLocation())).findFirst().orElse(null);

            if (ad == null) {
                System.out.println(AD_LOG_PREFIX + "Error: couldn't find advert for song");
                return;
            }

            System.out.println(AD_LOG_PREFIX + "Ran advert: " + ad.getTitle());

            textChannel.sendMessage(ad.constructAdvertMessage().setTimestamp(OffsetDateTime.now()).build()).queue();
        }
    }
}
