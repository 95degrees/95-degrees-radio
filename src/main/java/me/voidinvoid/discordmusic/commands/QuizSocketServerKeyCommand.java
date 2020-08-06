package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.quiz.QuizManager;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Rank;

public class QuizSocketServerKeyCommand extends Command {

    QuizSocketServerKeyCommand() {
        super("quiz-key", "Outputs a key to use when using the quiz manager dashboard", null, Rank.STAFF);
    }

    @Override
    public void invoke(CommandData data) {
        data.success("Key: ||" + Radio.getInstance().getService(QuizManager.class).getServerCode() + "||\n**⚠ This code should be kept private. Restart the bot to regenerate**");
    }
}
