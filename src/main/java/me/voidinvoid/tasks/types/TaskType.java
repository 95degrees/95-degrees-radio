package me.voidinvoid.tasks.types;

import me.voidinvoid.tasks.Parameter;
import me.voidinvoid.tasks.RadioTaskExecutor;

import static me.voidinvoid.tasks.Parameter.*;

@SuppressWarnings("unused")
public enum TaskType {

    PLAY_SONG(new PlaySongTask(false), string("song_name"), bool("remote", false), bool("play_instantly", false)),
    PAUSE_SONG(new PauseSongTask(true)),
    RESUME_SONG(new PauseSongTask(false)),
    ENABLE_SUGGESTIONS(new ToggleSuggestionsTask(true)),
    DISABLE_SUGGESTIONS(new ToggleSuggestionsTask(false)),
    SKIP_SONG(new SkipSongTask()),
    SWITCH_PLAYLIST(new SwitchPlaylistTask(), string("playlist_name"), bool("switch_instantly", false)),
    PLAY_JINGLE(new PlaySongTask(true), string("song_name", null), bool("play_instantly", true)),
    PLAY_SPECIAL(new PlaySpecialTask(), string("song_name"), bool("play_instantly", false), string("listening_to", null)), //play a song from /Special folder
    ANNOUNCE(new AnnounceTask(), string("message"), integer("delete_after", 0), string("announce_to_channel", null), bool("announce_to_dj_channel", true), bool("announce_to_text_channel", true), integer("colour", -1)), //white
    CLEAR_QUEUE(new ClearQueueTask()),
    START_KARAOKE(new KaraokeTask(true)),
    STOP_KARAOKE(new KaraokeTask(false)),
    RUN_ADVERT(new AdvertisementTask());

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
