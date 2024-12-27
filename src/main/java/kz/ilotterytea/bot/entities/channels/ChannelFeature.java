package kz.ilotterytea.bot.entities.channels;

import java.util.Arrays;
import java.util.Optional;

/**
 * Channel features.
 *
 * @author ilotterytea
 * @version 1.4
 */
public enum ChannelFeature {
    NOTIFY_7TV("notify_7tv_events"),
    SILENT_MODE("silent_mode");

    private String id;

    ChannelFeature(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static Optional<ChannelFeature> getById(String id) {
        return Arrays.stream(ChannelFeature.values()).filter((x) -> x.id.equals(id)).findFirst();
    }
}
