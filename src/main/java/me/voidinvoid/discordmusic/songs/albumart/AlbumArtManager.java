package me.voidinvoid.discordmusic.songs.albumart;

import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;

public class AlbumArtManager implements RadioService {

    private AlbumArt fallbackAlbumArt, jingleAlbumArt, networkAlbumArt, advertAlbumArt, specialAlbumArt;

    @Override
    public void onLoad() {
        var imgs = RadioConfig.config.images;

        fallbackAlbumArt = new RemoteAlbumArt(imgs.fallbackAlbumArt);
        jingleAlbumArt = new RemoteAlbumArt(imgs.jingleAlbumArt);
        networkAlbumArt = new RemoteAlbumArt(imgs.networkAlbumArt);
        advertAlbumArt = new RemoteAlbumArt(imgs.advertAlbumArt);
        specialAlbumArt = new RemoteAlbumArt(imgs.specialAlbumArt);
    }

    public AlbumArt getFallbackAlbumArt() {
        return fallbackAlbumArt;
    }

    public AlbumArt getJingleAlbumArt() {
        return jingleAlbumArt;
    }

    public AlbumArt getNetworkAlbumArt() {
        return networkAlbumArt;
    }

    public AlbumArt getAdvertAlbumArt() {
        return advertAlbumArt;
    }

    public AlbumArt getSpecialAlbumArt() {
        return specialAlbumArt;
    }
}
