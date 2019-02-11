package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.levelling.LevelExtras;
import me.voidinvoid.discordmusic.levelling.LevellingManager;
import me.voidinvoid.discordmusic.songs.SongType;
import me.voidinvoid.discordmusic.utils.ChannelScope;

import java.util.HashMap;
import java.util.Map;

public class SkipCommand extends Command {

    SkipCommand() {
        super("skip", "Skips the currently playing track", null, ChannelScope.RADIO_AND_DJ_CHAT);
    }

    private Map<String, Long> skipCooldowns = new HashMap<>();

    @Override
    public void invoke(CommandData data) {
        if (data.getTextChannel() == null || ChannelScope.DJ_CHAT.check(data.getTextChannel())) {
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

            var vc = data.getTextChannel().getGuild().getSelfMember().getVoiceState().getChannel();

            if (vc.getMembers().size() <= 2) { //the radio + the listener
                var orch = Radio.getInstance().getOrchestrator();

                var s = orch.getCurrentSong();

                if (s != null && s.getType() != SongType.SONG) {
                    data.error("You can only use this command when a song is playing");
                    return;
                }

                skipCooldowns.put(data.getMember().getUser().getId(), System.currentTimeMillis() + 15000); //15 secs
                orch.playNextSong();

                data.success("Skipped to the next song");
                return;
            }

            data.error("You must be alone in the radio voice channel to skip songs");
        }
    }
}
