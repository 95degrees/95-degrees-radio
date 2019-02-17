package me.voidinvoid.discordmusic.utils;

import me.voidinvoid.discordmusic.Radio;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
public final class Service {

    public static <T> T of(Class<T> service) {
        return Radio.getInstance().getService(service);
    }
}
