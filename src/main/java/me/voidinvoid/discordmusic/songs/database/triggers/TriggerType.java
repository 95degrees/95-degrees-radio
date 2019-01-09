package me.voidinvoid.discordmusic.songs.database.triggers;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.tasks.ParameterList;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;

public enum TriggerType {
    NOTIFY_USERS_WITH_ROLE((s, p) -> {
        String role = p.get("role_id", String.class);
        String message = p.get("message", String.class);

        Guild g = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat).getGuild();
        Role r = g.getRoleById(role);

        int i = 0;
        for (Member m : g.getMembersWithRoles(r)) {
            if (++i >= 75) {
                return; //try not to get rate limited
            }
            m.getUser().openPrivateChannel().queue(pm -> pm.sendMessage(message).queue());
        }
    }),
    SEND_MESSAGE_TO_CHANNEL((s, p) -> {
        String channel = p.get("channel_id", String.class);
        String message = p.get("message", String.class);

        TextChannel c = Radio.getInstance().getJda().getTextChannelById(channel);
        if (c == null) {
            System.out.println("Warning: text channel is null for SEND_MESSAGE_TO_CHANNEL action");
            return;
        }

        c.sendMessage(message).queue();
    });

    private TriggerAction onTrigger;

    TriggerType(TriggerAction onTrigger) {

        this.onTrigger = onTrigger;
    }

    public void onTrigger(Song song, ParameterList params) {
        onTrigger.onTrigger(song, params);
    }
}
