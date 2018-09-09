package me.voidinvoid.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.songs.AlbumArtType;
import me.voidinvoid.songs.Song;
import me.voidinvoid.songs.SongType;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.requests.restaction.MessageAction;

import java.io.File;

public final class FormattingUtils {

    public static String getFormattedMsTime(long time) {
        long second = (time / 1000) % 60;
        long minute = (time / (1000 * 60)) % 60;
        long hours = (time / (1000 * 60 * 60)) % 60;

        return (hours > 0 ? String.format("%02d:", hours) : "") + String.format("%02d:%02d", minute, second);
    }

    public static String getFormattedMsTimeLabelled(long time) {
        long seconds = (time / 1000) % 60;
        long minutes = (time / (1000 * 60)) % 60;
        long hours = (time / (1000 * 60 * 60)) % 60;

        String format = "";

        if (hours > 0) {
            format += hours + "h ";
        }

        if (hours == 0 || minutes > 0) {
            format += minutes + "m ";
        }

        if (minutes == 0 || seconds > 0) {
            format += seconds + "s";
        }

        return format.trim();
    }

    public static String escapeMarkup(String text) {
        if (text == null) return "";
        if (text.startsWith("http://") || text.startsWith("https://")) return text; //todo HACK
        return text.replace("_", "\\_").replace("*", "\\*").replace("~", "\\~").replace("`", "\\`");
    }

    public static String getSongType(AudioTrack track) {
        Song song = track.getUserData(Song.class);

        if (song.getType() == SongType.SONG && track.getInfo().isStream) return "Stream";

        return song.getType().getDisplayName();
    }
}
