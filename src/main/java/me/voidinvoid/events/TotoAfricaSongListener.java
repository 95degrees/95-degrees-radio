package me.voidinvoid.events;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.Radio;
import me.voidinvoid.songs.Song;
import net.dv8tion.jda.core.entities.*;

public class TotoAfricaSongListener implements SongEventListener {

    private TextChannel textChannel;
    private Role mentionRole;

    public TotoAfricaSongListener(TextChannel textChannel) {
        this.textChannel = textChannel;
        this.mentionRole = textChannel.getGuild().getRoleById("489858281389424680");
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        if (Radio.instance.getOrchestrator().getActivePlaylist().isDirectMessageNotifications() && track instanceof LocalAudioTrack && track.getInfo().title.equals("Africa") && track.getInfo().author.equals("Toto")) {
            int i = 0;
            for (Member m : textChannel.getGuild().getMembersWithRoles(mentionRole)) {
                if (++i >= 75) {
                    return; //try not to get rate limited
                }
                m.getUser().openPrivateChannel().queue(p -> p.sendMessage("Toto - Africa is now playing on the radio!").queue());
            }
        }
    }
}
