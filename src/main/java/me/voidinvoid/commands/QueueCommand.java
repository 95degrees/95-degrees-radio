package me.voidinvoid.commands;

import me.voidinvoid.DiscordRadio;

public class QueueCommand extends Command {

    QueueCommand() {
        super("queue", "Lists the next 10 songs in the queue", null, CommandScope.RADIO_AND_DJ_CHAT, "rq");
    }

    @Override
    public void invoke(CommandData data) {
        data.getTextChannel().sendMessage("```" + DiscordRadio.instance.getOrchestrator().getActivePlaylist().getSongs().getFormattedQueue().replaceAll("`", "") + "```").queue();
    }
}