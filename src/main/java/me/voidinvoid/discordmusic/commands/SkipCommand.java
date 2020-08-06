package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.levelling.Achievement;
import me.voidinvoid.discordmusic.levelling.AchievementManager;
import me.voidinvoid.discordmusic.levelling.LevelExtras;
import me.voidinvoid.discordmusic.levelling.LevellingManager;
import me.voidinvoid.discordmusic.songs.SongType;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Service;
import net.dv8tion.jda.api.entities.GuildChannel;

import java.util.HashMap;
import java.util.Map;

public class SkipCommand extends Command {

    SkipCommand() {
        super("skip", "Skips the currently playing track", null, null);
    }

    private Map<String, Long> skipCooldowns = new HashMap<>();

    @Override
    public void invoke(CommandData data) {
        if (data.getTextChannel() == null || ChannelScope.DJ_CHAT.check((GuildChannel) data.getTextChannel())) {
            Radio.getInstance().getOrchestrator().playNextSong();

            data.success("Skipped to the next track");
        } else {
            var canSkip = (boolean) Radio.getInstance().getService(LevellingManager.class).getLatestExtra(data.getMember().getUser(), LevelExtras.SKIP_SONGS_WHEN_ALONE).getValue();
            if (!canSkip) {
                data.error("You can't skip songs yet. You can unlock this ability when alone by levelling up, by listening to the radio more.\nUse `!level` to view your current level");
                return;
            }

            if (skipCooldowns.getOrDefault(data.getMember().getUser().getId(), 0L) > System.currentTimeMillis()) {
                data.error("Please wait before using this command again");
                return;
            }

            var vs = Radio.getInstance().getGuild().getSelfMember().getVoiceState();
            if (vs == null || vs.getChannel() == null) {
                data.error("Couldn't find the voice channel?");
                return;
            }

            var vc = vs.getChannel();

            if (vc.getMembers().size() <= 2) { //the radio + the listener
                var orch = Radio.getInstance().getOrchestrator();

                var s = orch.getCurrentSong();

                if (s != null && s.getType() != SongType.SONG) {
                    if (s.getType() == SongType.ADVERTISEMENT) Service.of(AchievementManager.class).rewardAchievement(data.getMember().getUser(), Achievement.SKIP_ADVERT);
                    data.error("You can only use this command when a song is playing");
                    return;
                }

                skipCooldowns.put(data.getMember().getUser().getId(), System.currentTimeMillis() + 15000); //15 secs
                orch.playNextSong();

                data.sendMessage("⏩ | Skipped to the next song");

                return;
            }

            data.error("You must be alone in the radio voice channel to skip songs");
        }
    }
}
