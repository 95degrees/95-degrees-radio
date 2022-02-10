package me.voidinvoid.discordmusic.rpc;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.SongOrchestrator;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.levelling.ListeningTrackerManager;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import net.dv8tion.jda.api.entities.User;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
public class IdentityInfo {

    public UserInfo user;
    public int maxSuggestions;
    public long maxQueueLength;
    public boolean canSkipSongs;
    public boolean isDj;

    public IdentityInfo(UserInfo user, int maxSuggestions, long maxQueueLength, boolean canSkipSongs, boolean isDj) {

        this.user = user;
        this.maxSuggestions = maxSuggestions;
        this.maxQueueLength = maxQueueLength;
        this.canSkipSongs = canSkipSongs;
        this.isDj = isDj;
    }

    public IdentityInfo(User user) {

        this.user = new UserInfo(user);

        var lm = Radio.getInstance().getService(ListeningTrackerManager.class);

        this.maxSuggestions = SongOrchestrator.MAX_CONCURRENT_SUGGESTIONS;
        this.maxQueueLength = SongOrchestrator.MAX_SONG_LENGTH;
        this.canSkipSongs = true;

        var guild = Radio.getInstance().getGuild();
        var member = guild.retrieveMember(user).onErrorMap(m -> null).complete();

        this.isDj = member != null && ChannelScope.DJ_CHAT.hasAccess(member);
    }
}
