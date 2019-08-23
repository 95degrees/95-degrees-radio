package me.voidinvoid.discordmusic.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.SongType;
import net.dv8tion.jda.api.entities.Message;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class Formatting {

    public static final List<String> NUMBER_EMOTES = Arrays.asList(new String(new char[]{49, 8419}), new String(new char[]{50, 8419}), new String(new char[]{51, 8419}), new String(new char[]{52, 8419}), new String(new char[]{53, 8419}), new String(new char[]{54, 8419}), new String(new char[]{55, 8419}), new String(new char[]{56, 8419}), new String(new char[]{57, 8419}), "🔟");
    private static final String MESSAGE_DIRECT_LINK_URL = "https://discordapp.com/channels/"; // /guild/channel/message/

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

    public static String getFormattedMinsTimeLabelled(int mins) {

        int minutes = mins % 60;
        int hours = mins / 60;

        String format = "";

        if (hours > 0) {
            format += hours + "h ";
        }

        if (hours == 0 || minutes > 0) {
            format += minutes + "m";
        }

        return format.trim();
    }

    public static String escape(String text) {
        if (text == null) return "";
        if (text.startsWith("http://") || text.startsWith("https://")) return text; //todo HACK
        return text.replace("_", "\\_").replace("*", "\\*").replace("||", "\\||").replace("~", "\\~").replace("`", "\\`");
    }

    public static String padString(String text, int amount) {
        if (text.length() > amount) return text.substring(0, amount);

        String padding = new String(new char[amount]).replace("\0", " ");
        return text + padding.substring(text.length());
    }

    public static String getDirectMessageLink(Message m) {
        if (m.getTextChannel() != null) {
            return MESSAGE_DIRECT_LINK_URL + m.getGuild().getId() + "/" + m.getChannel().getId() + "/" + m.getId();
        }

        return null;
    }

    public static String maskLink(String link, String mask) {
        return "[" + mask + "](" + link + ")";
    }

    public static String maskLink(Message msg, String mask) {
        return maskLink(getDirectMessageLink(msg), mask);
    }

    public static String getSongType(AudioTrack track) {
        Song song = track.getUserData(Song.class);

        if (song.getType() == SongType.SONG && track.getInfo().isStream) return "Stream";

        return song.getType().getDisplayName();
    }
}
