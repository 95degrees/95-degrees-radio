package me.voidinvoid.commands;

import me.voidinvoid.Radio;
import me.voidinvoid.utils.ChannelScope;

public class QueueCommand extends Command {

    QueueCommand() {
        super("queue", "Lists the next 10 songs in the queue", null, ChannelScope.RADIO_AND_DJ_CHAT, "rq");
    }

    @Override
    public void invoke(CommandData data) {
        data.getTextChannel().sendMessage("```" + Radio.instance.getOrchestrator().getActivePlaylist().getSongs().getFormattedQueue().replaceAll("`", "") + "```").queue();
    }
}