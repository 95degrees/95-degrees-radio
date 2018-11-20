package me.voidinvoid.discordmusic.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import net.dv8tion.jda.core.audio.AudioSendHandler;

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

    public byte[] provide20MsAudio() {
        return lastFrame.getData();
    }

    public boolean isOpus() {
        return true;
    }
}
