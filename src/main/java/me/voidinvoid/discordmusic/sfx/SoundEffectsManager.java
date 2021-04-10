package me.voidinvoid.discordmusic.sfx;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;

import java.util.function.Consumer;

public class SoundEffectsManager implements RadioService {

    public AudioTrack SWOOSH_SOUND_EFFECT;

    @Override
    public void onLoad() { //TODO no hardcode
        loadSoundEffect("/home/discord/radio/sfx/skip.ogg", t -> SWOOSH_SOUND_EFFECT = t);
    }

    private void loadSoundEffect(String identifier, Consumer<AudioTrack> result) {
        Radio.getInstance().getOrchestrator().getAudioManager().loadItem(identifier, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                result.accept(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                result.accept(null);
            }

            @Override
            public void noMatches() {
                result.accept(null);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                log("Failed to load sound effect");
                result.accept(null);
                exception.printStackTrace();
            }
        });
    }
}
