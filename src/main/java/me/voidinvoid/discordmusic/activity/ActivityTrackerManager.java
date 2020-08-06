package me.voidinvoid.discordmusic.activity;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import net.dv8tion.jda.api.entities.Member;

import java.util.List;

/**
 * DiscordMusic - 22/07/2020
 * This code was developed by VoidInVoid / Exfusion
 * © 2020
 */

public class ActivityTrackerManager implements RadioService {



    public List<Member> getListeners(ListeningContext context) {
        var guild = Radio.getInstance().getGuild();

        for (var vs : guild.getVoiceStates()) {

        }

        return null; //todo
    }
}
