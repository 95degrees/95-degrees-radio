package me.voidinvoid.discordmusic.commands;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.SongOrchestrator;
import me.voidinvoid.discordmusic.cache.YouTubeCacheManager;
import me.voidinvoid.discordmusic.coins.RadioAwardsManager;
import me.voidinvoid.discordmusic.guardian.GuardianIntegrationManager;
import me.voidinvoid.discordmusic.levelling.Achievement;
import me.voidinvoid.discordmusic.levelling.AchievementManager;
import me.voidinvoid.discordmusic.levelling.ListeningTrackerManager;
import me.voidinvoid.discordmusic.lyrics.LiveLyricsManager;
import me.voidinvoid.discordmusic.quiz.QuizManager;
import me.voidinvoid.discordmusic.restream.RadioRestreamManager;
import me.voidinvoid.discordmusic.rpc.RPCSocketManager;
import me.voidinvoid.discordmusic.utils.Rank;
import me.voidinvoid.discordmusic.utils.Service;
import net.dv8tion.jda.api.JDAInfo;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DebugCommand extends Command {

    DebugCommand() {
        super("debug", "Runs debug actions", null, Rank.STAFF);
    }

    @Override
    public void invoke(CommandData data) {

        if (data.getArgs().length > 0) {
            DebugAction action = null;
            try {
                action = DebugAction.valueOf(data.getArgs()[0].toUpperCase());
            } catch (Exception ignored) {
            }

            if (action != null) {
                data.success("Invoking " + action.name());
                action.invoke(data);
                return;
            }
        }
        data.error("Debug action required. Available actions:\n`" + Arrays.stream(DebugAction.values()).map(Enum::name).collect(Collectors.joining("\n")) + "`");
    }

    public enum DebugAction {

        LIST_ACTIVE_CLIENTS(d -> {
            d.success(Radio.getInstance().getService(RPCSocketManager.class).getServer().getAllClients().stream().map(c -> c.getSessionId().toString()).collect(Collectors.joining(", ")));
        }),
        RESTREAM(d -> {
            var id = d.getArgs()[1]; //text channel

            Service.of(RadioRestreamManager.class).joinVoiceChannel(id);
        }),
        RESTREAM_LEAVE(d -> {
            Service.of(RadioRestreamManager.class).leaveVoice(d.getMember().getGuild().getId());
        }),
        REWARD(d -> {
            Service.of(RadioAwardsManager.class).pushAward();
        }),
        QUIZ_PROGRESS(d -> {
            if (Radio.getInstance().getService(QuizManager.class).getActiveQuiz().progress(false)) {
                Radio.getInstance().getOrchestrator().playNextSong();
            }
        }),
        GRANT_ACHIEVEMENT(d -> {
            Achievement a = Achievement.valueOf(d.getArgs()[1]);
            Radio.getInstance().getService(AchievementManager.class).rewardAchievement(d.getMember().getUser(), a);
        }),
        LEVEL_UP(d -> {
            int i = d.getArgs().length < 2 ? 1 : Integer.parseInt(d.getArgs()[1]);
            d.success("Level up x" + i);
            Radio.getInstance().getService(ListeningTrackerManager.class).rewardExperience(d.getMember().getUser(), i);
        }),
        LYRICS(d -> {
            var sm = Service.of(LiveLyricsManager.class);

            sm.setEnabled(!sm.isEnabled());

            d.success("Lyrics scraping " + (sm.isEnabled() ? "enabled" : "disabled"));
        }),
        VERSIONS(d -> {
            d.success("Version info:\nJDA: " + JDAInfo.VERSION + "\nLavaplayer: " + PlayerLibrary.VERSION);
        }),
        DOWNLOAD(d -> {
            Service.of(YouTubeCacheManager.class).loadOrCache(d.getArgs()[1]);
            d.success("Check console for progress");
        }),
        GUARDIAN_XP(d -> {
            Service.of(GuardianIntegrationManager.class).addGuardianExperience(d.getMember().getId(), 100, d.getTextChannel().getId());
        }),
        TIMESCALE(d -> {
            Radio.getInstance().getOrchestrator().setTimescale(Float.parseFloat(d.getArgs()[1]));
        }),
        PITCH(d -> {
            Radio.getInstance().getOrchestrator().setPitch(Float.parseFloat(d.getArgs()[1]));
        });

        private Consumer<CommandData> action;

        DebugAction(Consumer<CommandData> action) {

            this.action = action;
        }

        public void invoke(CommandData data) {
            action.accept(data);
        }
    }
}
