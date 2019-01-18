package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.levelling.Achievement;
import me.voidinvoid.discordmusic.levelling.AchievementManager;
import me.voidinvoid.discordmusic.quiz.QuizManager;
import me.voidinvoid.discordmusic.rpc.RPCSocketManager;
import me.voidinvoid.discordmusic.utils.ChannelScope;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DebugCommand extends Command {

    DebugCommand() {
        super("debug", "Runs debug actions", null, RadioConfig.config.debug ? ChannelScope.RADIO_AND_DJ_CHAT : ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {

        if (data.getArgs().length > 0) {
            DebugAction action = null;
            try {
                action = DebugAction.valueOf(data.getArgs()[0].toUpperCase());
            } catch (Exception ignored) {}

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
        TEST_QUIZ(d -> {

        }),
        QUIZ_PROGRESS(d -> {
            if (Radio.getInstance().getService(QuizManager.class).getActiveQuiz().progress(false)) {
                Radio.getInstance().getOrchestrator().playNextSong();
            }
        }),
        GRANT_ACHIEVEMENT(d -> {
            Achievement a = Achievement.valueOf(d.getArgs()[1]);
            Radio.getInstance().getService(AchievementManager.class).rewardAchievement(d.getMember().getUser(), a);
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
