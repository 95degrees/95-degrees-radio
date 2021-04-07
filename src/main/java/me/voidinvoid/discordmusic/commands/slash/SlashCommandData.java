package me.voidinvoid.discordmusic.commands.slash;

import me.voidinvoid.discordmusic.utils.Colors;
import me.voidinvoid.discordmusic.utils.Emoji;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.Objects;

public class SlashCommandData {

    private final SlashCommandEvent event;
    private final CommandUpdateAction.CommandData command;

    public SlashCommandData(SlashCommandEvent event, CommandUpdateAction.CommandData command) {

        this.event = event;
        this.command = command;
    }

    @NotNull
    public Member getMember() {
        return Objects.requireNonNull(event.getMember());
    }

    public TextChannel getTextChannel() {
        return event.getTextChannel();
    }

    public SlashCommandEvent.OptionData getOption(String name) {
        return event.getOption(name);
    }

    public String getStringOption(String name, String defaultValue) {
        var opt = getOption(name);

        return opt == null ? defaultValue : opt.getAsString();
    }

    public long getLongOption(String name, long defaultValue) {
        var opt = getOption(name);

        return opt == null ? defaultValue : opt.getAsLong();
    }

    public boolean getBooleanOption(String name, boolean defaultValue) {
        var opt = getOption(name);

        return opt == null ? defaultValue : opt.getAsBoolean();
    }

    public <T extends AbstractChannel> T getChannelOption(String name, T defaultValue, Class<T> channelClass) {
        var opt = getOption(name);

        return !channelClass.isInstance(opt) ? defaultValue : channelClass.cast(opt.getAsChannel());
    }

    public AbstractChannel getChannelOption(String name, AbstractChannel defaultValue) {
        return getChannelOption(name, defaultValue, AbstractChannel.class);
    }


    public Member getMemberOption(String name, Member defaultValue) {
        var opt = getOption(name);

        return opt == null ? defaultValue : opt.getAsMember();
    }

    public User getUserOption(String name, User defaultValue) {
        var opt = getOption(name);

        return opt == null ? defaultValue : opt.getAsUser();
    }


    public SlashCommandEvent getEvent() {
        return event;
    }

    public CommandUpdateAction.CommandData getCommand() {
        return command;
    }

    public void acknowledge(boolean ephemeral) {
        event.acknowledge().queue();
    }

    public void embed(MessageEmbed embed, boolean ephemeral) {
        event.reply(embed).setEphemeral(ephemeral).queue();
    }

    public void embed(MessageEmbed embed) {
        embed(embed, false);
    }

    public void success(String message) {
        embed(new EmbedBuilder()
                .setColor(Colors.ACCENT_MAIN)
                .setDescription(Emoji.TICK + " " + message)
                .setFooter(event.getUser().getName(), event.getUser().getAvatarUrl())
                .setTimestamp(OffsetDateTime.now()).build());
    }

    public void sendMessage(String message, boolean ephemeral) {
        event.reply(message).setEphemeral(ephemeral).queue();
    }

    public void sendMessage(String message) {
        sendMessage(message, false);
    }

    public void prefix(String prefix, String message) {
        sendMessage(prefix + " • " + message);
    }

    public void mention(String message, boolean ephemeral) {
        mention("", message, ephemeral);
    }

    public void mention(String prefix, String message, boolean ephemeral) {
        sendMessage((prefix == null || prefix.isEmpty() ? "" : prefix + " • ") + "**" + event.getUser().getName() + "**, " + message, ephemeral);
    }

    /*public MessageAction mentionAction(String prefix, String message) {
        String msg = (prefix == null || prefix.isEmpty() ? "" : prefix + " • ") + (member == null ? "Console" : "**" + member.getUser().getName() + "**") + ", " + message;
        if (textChannel == null) {
            System.out.println(msg);
            return null;
        }

        return textChannel.sendMessage(msg);
    }*/

    public void inDevelopment() {
        error("This feature is currently in development. Stay tuned!");
    }

    public void error(String message) {
        error(message, true);
    }

    public void error(String message, boolean ephemeral) {
        if (ephemeral) {
            sendMessage(Emoji.CROSS + " " + message, true);
        } else {
            createErrorEmbed(message).build();
        }
    }

    public void errorEmbed(String message) {
        embed(createErrorEmbed(message).build(), false);
    }

    public EmbedBuilder createErrorEmbed(String message) {
        var embed = new EmbedBuilder()
                .setTitle(Emoji.CROSS + " Command Error")
                .setColor(Color.RED)
                .setDescription(message)
                .setFooter(event.getUser().getName(), event.getUser().getAvatarUrl())
                .setTimestamp(OffsetDateTime.now());

        return embed;
    }
}
