package me.voidinvoid.discordmusic.rpc;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.songs.SongType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UpcomingEventsInfo {

    public static transient final UpcomingEvent JINGLE_EVENT = new UpcomingEvent("jingle", "A jingle will play after this song", "Postpone", () -> {
        Radio.getInstance().getOrchestrator().setTimeUntilJingle(RadioConfig.config.orchestration.jingleFrequency);
    });

    public static transient final UpcomingEvent ADVERT_EVENT = new UpcomingEvent("advert", "An advert will play after this song", "Cancel", () -> {
        Radio.getInstance().getOrchestrator().getAwaitingSpecialSongs().removeIf(s -> s.getType() == SongType.ADVERTISEMENT);
    });

    public static transient final UpcomingEvent PAUSE_EVENT = new UpcomingEvent("upcoming_pause", "The radio will pause after this song has finished", "Cancel", () -> {
        Radio.getInstance().getOrchestrator().setPausePending(false);
    });

    public static transient final UpcomingEvent REWARD_EVENT = new UpcomingEvent("upcoming_rewards", "Playing reward message after this song", "Cancel", () -> {
        Radio.getInstance().getOrchestrator().getAwaitingSpecialSongs().removeIf(s -> s.getType() == SongType.REWARD);
    });

    public List<UpcomingEvent> upcomingEvents = new ArrayList<>();

    public UpcomingEventsInfo(UpcomingEvent... events) {
        upcomingEvents.addAll(Arrays.asList(events));
    }

    public UpcomingEventsInfo addIf(UpcomingEvent event, boolean condition) {
        if (condition) upcomingEvents.add(event);

        return this;
    }
}
