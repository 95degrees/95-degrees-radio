package me.voidinvoid.discordmusic.remotecontrol;

import com.google.gson.Gson;
import io.socket.client.IO;
import io.socket.client.Socket;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

public class RemoteSocketControlManager implements RadioService {

    private Socket socket;
    private BotInfo info;

    @Override
    public void onLoad() {
        info = BotInfo.get();

        try {
            socket = IO.socket("http://51.15.48.213:9525");
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
            return;
        }

        socket.on(Socket.EVENT_CONNECT, args -> {
            try {
                socket.emit("register_bot", new JSONObject(new Gson().toJson(info)));
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        });

        socket.on("restart", args -> {
            try {
                if (!((String) args[0]).equalsIgnoreCase(info.id)) return;

                log("Running socket restart!");
                Radio.getInstance().shutdown(true);
            } catch (Exception ex) {
            }
        });

        socket.on("shutdown", args -> {
            try {
                if (!((String) args[0]).equalsIgnoreCase(info.id)) return;

                log("Running socket shutdown!");
                Radio.getInstance().shutdown(false);
            } catch (Exception ex) {
            }
        });

        new Thread(() -> {
            log("Starting socket connect...");
            socket.connect();
            log("Socket connect ended");
        }).start();
    }

    @Override
    public void onShutdown() {
        if (socket != null) {
            socket.disconnect();
            socket = null;
        }
    }
}
