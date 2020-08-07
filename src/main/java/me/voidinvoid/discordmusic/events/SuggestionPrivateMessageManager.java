package me.voidinvoid.discordmusic.events;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.levelling.Achievement;
import me.voidinvoid.discordmusic.levelling.AchievementManager;
import me.voidinvoid.discordmusic.songs.NetworkSong;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.UserSuggestable;
import me.voidinvoid.discordmusic.utils.Colors;
import me.voidinvoid.discordmusic.utils.Service;
import me.voidinvoid.discordmusic.utils.Songs;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.VoiceChannel;

import java.time.OffsetDateTime;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
public class SuggestionPrivateMessageManager implements RadioService, RadioEventListener {

    private VoiceChannel voiceChannel;

    @Override
    public void onLoad() {
        voiceChannel = Radio.getInstance().getJda().getVoiceChannelById(RadioConfig.config.channels.voice);
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        if (song instanceof UserSuggestable) {
            var s = (UserSuggestable) song;
            if (s.getSuggestedBy() != null) {
                if (voiceChannel.getMembers().stream().noneMatch(m -> m.getUser().getId().equals(s.getSuggestedBy().getId()))) { //user not in vc
                    var am = Service.of(AchievementManager.class);

                    if (am != null) am.rewardAchievement(s.getSuggestedBy(), Achievement.LEAVE_AFTER_SUGGESTION);

                    MessageEmbed pm = new EmbedBuilder()
                            .setTitle("Song Suggestion Reminder")
                            .setDescription(Songs.titleArtist(song) + " is now playing on 95 Degrees Radio!")
                            .setTimestamp(OffsetDateTime.now())
                            .setColor(Colors.ACCENT_MAIN)
                            .setFooter("🔔 Suggestion reminder", null)
                            .build();

                    s.getSuggestedBy().openPrivateChannel().queue(c -> c.sendMessage(pm).queue());
                }
            }
        }
    }
}
