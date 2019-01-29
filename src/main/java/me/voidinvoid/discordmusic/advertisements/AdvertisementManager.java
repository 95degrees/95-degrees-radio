package me.voidinvoid.discordmusic.advertisements;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.events.SongEventListener;
import me.voidinvoid.discordmusic.songs.NetworkSong;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.SongQueue;
import me.voidinvoid.discordmusic.songs.SongType;
import me.voidinvoid.discordmusic.songs.local.FileSong;
import me.voidinvoid.discordmusic.songs.local.LocalSongQueue;
import me.voidinvoid.discordmusic.utils.ConsoleColor;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.TextChannel;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdvertisementManager implements RadioService, SongEventListener {

    private static final String AD_LOG_PREFIX = ConsoleColor.CYAN_BACKGROUND + " AD " + ConsoleColor.RESET_SPACE;
    private static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    private List<Advertisement> adverts = new ArrayList<>();
    private int currentAdIndex;

    private TextChannel textChannel;

    @Override
    public void onLoad() {
        textChannel = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat);

        try {
            DatabaseManager db = Radio.getInstance().getService(DatabaseManager.class);
            if (db != null) {
                adverts = db.getCollection("adverts").find().map(d -> GSON.fromJson(d.toJson(), Advertisement.class)).into(new ArrayList<>());
            }
        } catch (Exception e) {
            System.out.println(AD_LOG_PREFIX + "Error loading adverts");
            e.printStackTrace();
        }

        adverts.forEach(Advertisement::generateSong);

        if (currentAdIndex > adverts.size()) {
            currentAdIndex = adverts.size() - 1;
        }
    }

    public void pushAdvertisement() {
        if (adverts.size() == 0) return;

        currentAdIndex++;
        if (currentAdIndex >= adverts.size()) currentAdIndex = 0;

        List<Song> awaitingSongs = Radio.getInstance().getOrchestrator().getAwaitingSpecialSongs();
        System.out.println(awaitingSongs);
        System.out.println(adverts.get(currentAdIndex));
        System.out.println(adverts.get(currentAdIndex).getSong());
        System.out.println(awaitingSongs.stream());
        if (awaitingSongs.stream().noneMatch(s -> s.getType() == SongType.ADVERTISEMENT)) { //only queue one ad at a time
            awaitingSongs.add(adverts.get(currentAdIndex).getSong());
        }
    }

    public List<Advertisement> getAdverts() {
        return adverts;
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        if (song.getType() == SongType.ADVERTISEMENT) {
            Advertisement ad = adverts.stream().filter(a -> a.getIdentifier().equalsIgnoreCase(song.getFileName())).findAny().orElse(null);

            if (ad == null) {
                System.out.println(AD_LOG_PREFIX + "Error: couldn't find advert for song");
                return;
            }

            System.out.println(AD_LOG_PREFIX + "Ran advert: " + ad.getTitle());

            textChannel.sendMessage(ad.constructAdvertMessage().setTimestamp(OffsetDateTime.now()).build()).queue();
        }
    }
}
