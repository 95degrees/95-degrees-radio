package me.voidinvoid;

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
        //AudioListener listener = SongOrchestrator.instance.getKaraokeAudioListener();
        /*ShortBuffer buff = ByteBuffer.allocateDirect(6000)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        AudioChunkDecoder decoder = lastFrame.getFormat().createDecoder();
        decoder.decode(data, buff);
        decoder.close();
        if (listener != null) listener.addSelfData(buff);*/
        return lastFrame.getData();
    }

    public boolean isOpus() {
        return true;
    }
}
