package me.voidinvoid.commands;

import me.voidinvoid.Radio;
import me.voidinvoid.utils.ChannelScope;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DebugCommand extends Command {

    DebugCommand() {
        super("debug", "Runs debug actions", null, ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        if (data.getArgs().length == 0) {
            data.error("Debug action required. Available actions:\n" + Arrays.stream(DebugAction.values()).map(Enum::name).collect(Collectors.joining("\n")));
        }

        Radio.instance.getAdvertisementManager().pushAdvertisement();
        data.success("Queued an advert");
    }

    public enum DebugAction {

        LIST_ACTIVE_CLIENTS(d -> {
            d.success(Radio.instance.getSocketServer().getServer().getAllClients().stream().map(c -> c.getSessionId().toString()).collect(Collectors.joining(", ")));
        }),
        TEST_QUIZ(d -> {

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
