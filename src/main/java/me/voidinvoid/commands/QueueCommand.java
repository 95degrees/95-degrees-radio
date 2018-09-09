package me.voidinvoid.commands;

public class QueueCommand extends Command {

    public QueueCommand() {
        super("queue", "Lists the next 10 songs in the queue", null, CommandScope.RADIO_AND_DJ_CHAT, "rq");
    }

    @Override
    public void invoke(CommandData data) {

    }
}
