package me.voidinvoid.discordmusic.cache;

import com.github.kiulian.downloader.OnYoutubeDownloadListener;
import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.model.formats.AudioFormat;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.utils.ConsoleColor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * DiscordMusic - 27/03/2020
 * This code was developed by VoidInVoid / Exfusion
 * © 2020
 */

public class YouTubeCacheManager implements RadioService {

    private static final String CACHE_PREFIX = ConsoleColor.GREEN_BACKGROUND_BRIGHT + " Cache ";

    private YoutubeDownloader downloader;
    private File cacheDir;

    @Override
    public void onLoad() {
        downloader = new YoutubeDownloader();

        cacheDir = Path.of(RadioConfig.config.locations.songCache).toFile();
    }

    @Override
    public String getLogPrefix() {
        return CACHE_PREFIX;
    }

    /**
     * Attempts to load this song's cached version, or null if it does not exist
     * If the cache doesn't already exist, it will create it for next time
     *
     * @param path the url of the song to cache
     * @return the cached file location, or null
     */
    public String loadOrCache(String path) {
        try {
            var urlSplit = path.split("\\?v=");

            if (urlSplit.length < 2) return null;

            var vidId = urlSplit[1];

            var targetPath = cacheDir.toPath().resolve(vidId);

            if (Files.exists(targetPath)) {
                log(vidId + " has been cached!");
                return targetPath.toString();
            }

            log("Looking up download sources for " + vidId + "...");

            var video = downloader.getVideo(vidId);

            var formats = video.audioFormats();

            if (formats.isEmpty()) {
                log("Couldn't find any audio formats!");
                return null;
            }

            formats.sort(Comparator.comparing(AudioFormat::audioQuality));

            log("Found " + formats.size() + " appropriate audio formats");

            var format = formats.get(0);

            log("OK! Now downloading " + vidId + " with " + format.audioQuality() + " audio quality...");

            video.downloadAsync(format, cacheDir, new OnYoutubeDownloadListener() {
                @Override
                public void onDownloading(int i) {
                }

                @Override
                public void onFinished(File file) {
                    try {
                        Files.move(file.toPath(), targetPath);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }

                    log("DONE! Cached video to " + targetPath + "!");
                }

                @Override
                public void onError(Throwable throwable) {
                    log("ERROR! Downloading " + vidId + ":");
                    throwable.printStackTrace();;
                }
            });

        } catch (Exception ex) {
            log("ERROR! Downloading YouTube video:");
            ex.printStackTrace();
        }

        return null;
    }
}
