package me.voidinvoid.discordmusic.utils.cache;

public interface ICached<T> {

    String getId();

    T get();
}
