package me.voidinvoid.discordmusic.rpc.routes;

import com.corundumstudio.socketio.listener.DataListener;
import me.voidinvoid.discordmusic.rpc.routes.middleware.RPCRouteMiddleware;

/**
 * DiscordMusic - 11/05/2020
 * This code was developed by VoidInVoid / Exfusion
 * © 2020
 */

public abstract class RPCRoute {

    public <T> RPCRoute(String internalName, Class<T> dataType, DataListener<T> listener, RPCRouteMiddleware... middleware) {

    }

    public RPCRoute(String internalName, DataListener<Object> listener, RPCRouteMiddleware... middleware) {

    }
}
