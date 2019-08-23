package me.voidinvoid.discordmusic.rpc;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.levelling.LevelExtras;
import me.voidinvoid.discordmusic.levelling.LevellingManager;
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

        var lm = Radio.getInstance().getService(LevellingManager.class);

        this.maxSuggestions = (int) lm.getLatestExtra(user, LevelExtras.MAX_SUGGESTIONS_IN_QUEUE).getValue();
        this.maxQueueLength = (long) lm.getLatestExtra(user, LevelExtras.MAX_SUGGESTION_LENGTH).getValue();
        this.canSkipSongs = (boolean) lm.getLatestExtra(user, LevelExtras.SKIP_SONGS_WHEN_ALONE).getValue();

        var guild = Radio.getInstance().getGuild();
        var djChannel = guild.getTextChannelById(RadioConfig.config.channels.djChat);

        this.isDj = djChannel.canTalk(guild.getMember(user));
    }
}
