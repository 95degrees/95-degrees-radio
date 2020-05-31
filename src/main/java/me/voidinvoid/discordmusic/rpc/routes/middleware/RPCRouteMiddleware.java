package me.voidinvoid.discordmusic.rpc.routes.middleware;

import com.corundumstudio.socketio.SocketIOClient;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.rpc.RPCSocketManager;
import me.voidinvoid.discordmusic.utils.Service;
import me.voidinvoid.discordmusic.utils.cache.CachedChannel;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.function.Function;

/**
 * DiscordMusic - 11/05/2020
 * This code was developed by VoidInVoid / Exfusion
 * © 2020
 */

public class RPCRouteMiddleware {

    public static final RPCRouteMiddleware CHECK_FOR_DJ_STATUS = new RPCRouteMiddleware(c -> {
        var mb = Service.of(RPCSocketManager.class).getIdentities().get(c);

        return mb != null && new CachedChannel<TextChannel>(RadioConfig.config.channels.djChat).get().canTalk(mb);
    });

    private final Function<SocketIOClient, Boolean> validate;

    public RPCRouteMiddleware(Function<SocketIOClient, Boolean> validate) {

        this.validate = validate;
    }

    public boolean validate(SocketIOClient client) {
        return validate.apply(client);
    }
}
