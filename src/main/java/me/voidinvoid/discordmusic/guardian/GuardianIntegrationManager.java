package me.voidinvoid.discordmusic.guardian;

import com.google.gson.Gson;
import io.socket.client.IO;
import io.socket.client.Socket;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.guardian.requests.AddExperienceRequest;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

/**
 * Radio - 13/12/2020
 * This code was developed by VoidInVoid / Exfusion
 * © 2020
 */

public class GuardianIntegrationManager implements RadioService {

    private static final String OUTBOUND_ADD_XP = "add_xp";
    private Socket socket;

    @Override
    public void onLoad() {
        if (socket == null) {
            try {
                socket = IO.socket("http://localhost:" + (RadioConfig.config.debug ? 10094 : 10095));
            } catch (URISyntaxException ex) {
                ex.printStackTrace();
                return;
            }

            new Thread(() -> {
                log("Starting socket connect...");
                socket.connect();
                log("Socket connect ended");
            }).start();
        }
    }

    public void addGuardianExperience(String memberId, int amount, String channelId) {
        try {
            socket.emit(OUTBOUND_ADD_XP, new JSONObject(new Gson().toJson(new AddExperienceRequest(memberId, channelId, amount))));
            log("Adding " + amount + " xp to " + memberId);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onShutdown() {
        if (socket != null) {
            socket.disconnect();
            socket = null;
        }
    }
}
