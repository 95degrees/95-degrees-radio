package me.voidinvoid.discordmusic.karaoke;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import de.sciss.jump3r.lowlevel.LameEncoder;
import me.voidinvoid.discordmusic.config.RadioConfig;
import net.dv8tion.jda.core.audio.AudioReceiveHandler;
import net.dv8tion.jda.core.audio.CombinedAudio;
import net.dv8tion.jda.core.audio.UserAudio;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

@Deprecated //for now
public class AudioListener implements AudioReceiveHandler {

    private boolean recording;

    private boolean empty = true;

    private AudioTrack song;

    private ByteArrayOutputStream output;

    //private short[] selfData;

    public void startRecording(AudioTrack song) {
        recording = true;
        empty = true;

        this.song = song;

        output = new ByteArrayOutputStream();
    }

    public void stopRecording(boolean save) {
        recording = false;
        ByteArrayOutputStream out = output;
        String title = this.song == null ? "???" : song.getInfo().title;

        Thread t = new Thread(() -> {
            try {
                if (save && !empty) {
                    //RequestFuture<Message> msg = Radio.getInstance().getOrchestrator().onRecordingStarted(title);

                    try (ByteArrayOutputStream encOutput = new ByteArrayOutputStream()) {

                        LameEncoder encoder = new LameEncoder(OUTPUT_FORMAT, 64, LameEncoder.CHANNEL_MODE_AUTO, LameEncoder.QUALITY_HIGHEST, true);

                        byte[] all = out.toByteArray();
                        for (int i = 0; i < all.length; i += 20) {
                            byte[] sect = Arrays.copyOfRange(all, i, Math.min(all.length, i + 20));
                            byte[] buffer = new byte[encoder.getPCMBufferSize()];

                            encoder.encodeBuffer(sect, 0, sect.length, buffer);
                            encOutput.write(trim(buffer));
                        }

                        encoder.close();

                        String fileName = Paths.get(RadioConfig.config.locations.recordings, System.currentTimeMillis() + ".mp3").toString();

                        try (FileOutputStream fileOut = new FileOutputStream(fileName)) {
                            encOutput.writeTo(fileOut);
                        }

                        //Radio.getInstance().getOrchestrator().onRecordingReady(msg.get(), title, fileName);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        t.setDaemon(true);
        t.start();
    }

    private byte[] trim(byte[] bytes) {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0) {
            --i;
        }

        return Arrays.copyOf(bytes, i + 1);
    }

    @Override
    public boolean canReceiveCombined() {
        return true;
    }

    @Override
    public boolean canReceiveUser() {
        return false;
    }

    @Override
    public void handleCombinedAudio(CombinedAudio audio) {
        if (!recording) return;

        if (empty && !audio.getUsers().isEmpty()) empty = false;

        try {
            /*byte[] data = audio.getAudioData(1);
            if (selfData != null) {
                for (int i = 0; i < Math.min(data.length, selfData.length); i++) {
                    byte[] convSelfData = convertToLeftRightAudio(selfData);
                    if ((short)convSelfData[i] + (short) data[i] > Byte.MAX_VALUE) {
                        data[i] = Byte.MAX_VALUE;
                    } else {
                        data[i] += convSelfData[i];
                    }
                }
            }*/
            output.write(audio.getAudioData(1));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*public byte[] convertToLeftRightAudio(short[] audioData) {
        int byteIndex = 0;
        byte[] audio = new byte[audioData.length * 2];

        for(int i = 0; i < audioData.length; ++i) {
            short s = audioData[i];

            byte leftByte = (byte)(255 & s >> 8);
            byte rightByte = (byte)(255 & s);
            audio[byteIndex] = leftByte;
            audio[byteIndex + 1] = rightByte;
            byteIndex += 2;
        }

        return audio;
    }*/

    @Override
    public void handleUserAudio(UserAudio audio) {

    }

    public boolean isRecording() {
        return recording;
    }

    /*public void addSelfData(ShortBuffer buffer) {
        short[] shortArray = new short[1920];
        buffer.get(shortArray);
        selfData = shortArray;
    }*/
}
