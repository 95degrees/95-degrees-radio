package me.voidinvoid.discordmusic.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import net.dv8tion.jda.api.audio.AudioSendHandler;

import java.nio.ByteBuffer;

public class AudioPlayerSendHandler implements AudioSendHandler {

    private AudioPlayer player;
    private AudioFrame lastFrame;

    public AudioPlayerSendHandler(AudioPlayer player) {
        this.player = player;
    }

    public boolean canProvide() {
        lastFrame = player.provide();
        return lastFrame != null;
    }

    public ByteBuffer provide20MsAudio() {
        return ByteBuffer.wrap(lastFrame.getData());
    }

    public boolean isOpus() {
        return true;
    }

    //bit of a hack but it works
    public static class RestreamAudioPlayerSendHandler implements AudioSendHandler {

        private final AudioPlayerSendHandler basePlayer;

        public RestreamAudioPlayerSendHandler(AudioPlayerSendHandler basePlayer) {

            this.basePlayer = basePlayer;
        }

        public boolean canProvide() { //not calling player.provide() !!
            return basePlayer.lastFrame != null;
        }

        public ByteBuffer provide20MsAudio() {
            return basePlayer.provide20MsAudio();
        }

        @Override
        public boolean isOpus() {
            return true;
        }
    }
}