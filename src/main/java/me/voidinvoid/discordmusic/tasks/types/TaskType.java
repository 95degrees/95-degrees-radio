package me.voidinvoid.discordmusic.tasks.types;

import me.voidinvoid.discordmusic.stats.Statistic;
import me.voidinvoid.discordmusic.tasks.Parameter;
import me.voidinvoid.discordmusic.tasks.RadioTaskExecutor;

import static me.voidinvoid.discordmusic.tasks.Parameter.*;

@SuppressWarnings("unused")
public enum TaskType {

    PLAY_SONG(new PlaySongTask(false), of("song_name"), of("remote", false), of("play_instantly", false)),
    PAUSE_SONG(new PauseSongTask(true)),
    RESUME_SONG(new PauseSongTask(false)),
    ENABLE_SUGGESTIONS(new ToggleSuggestionsTask(true)),
    DISABLE_SUGGESTIONS(new ToggleSuggestionsTask(false)),
    SKIP_SONG(new SkipSongTask()),
    SWITCH_PLAYLIST(new SwitchPlaylistTask(), of("playlist_name"), of("switch_instantly", false)),
    PLAY_JINGLE(new PlaySongTask(true), of("song_name", null), of("play_instantly", true)),
    PLAY_SPECIAL(new PlaySpecialTask(), of("song_name"), of("play_instantly", false), of("listening_to", null)), //play a song from /Special folder
    ANNOUNCE(new AnnounceTask(), of("event_subscription_id", null), of("title", "Announcement"), of("message"), of("image_url", null), of("delete_after", 0), of("announce_to_channel", null), of("announce_to_dj_channel", true), of("announce_to_text_channel", true), of("colour", -1)), //white
    CLEAR_QUEUE(new ClearQueueTask()),
    RUN_ADVERT(new AdvertisementTask()),
    ENABLE_QUEUE_COMMAND(new ToggleQueueTask(true)),
    DISABLE_QUEUE_COMMAND(new ToggleQueueTask(false)),
    LEADERBOARD_REWARD(new LeaderboardRewardTask(), of("type", Statistic.LISTEN_TIME.name()), of("reward", 100)),
    REWARD(new RewardTask());

    private RadioTaskExecutor executor;
    private Parameter[] params;

    TaskType(RadioTaskExecutor task, Parameter... params) {
        this.executor = task;
        this.params = params;
    }

    public RadioTaskExecutor getExecutor() {
        return executor;
    }

    public Parameter[] getParams() {
        return params;
    }
}
