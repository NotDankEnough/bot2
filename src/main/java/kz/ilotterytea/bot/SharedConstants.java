package kz.ilotterytea.bot;

import kz.ilotterytea.bot.utils.HibernateUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * App constants.
 *
 * @author ilotterytea
 * @since 1.0
 */
public class SharedConstants {
    public static final String APP_NAME = "huinyabot";
    public static final byte APP_VERSION_MAJOR = 1;
    public static final byte APP_VERSION_MINOR = 5;
    public static final byte APP_VERSION_PATCH = 0;

    public static String getVersion() {
        return String.format("%s.%s.%s", APP_VERSION_MAJOR, APP_VERSION_MINOR, APP_VERSION_PATCH);
    }

    public static final String PROPERTIES_PATH = "config.properties";

    public static final String TWITCH_TOKEN;
    public static final String TWITCH_DEFAULT_PREFIX;
    public static final String TWITCH_DEFAULT_LOCALE_ID;

    public static final Properties PROPERTIES;

    static {
        Properties properties = new Properties();

        try (FileInputStream fileInputStream = new FileInputStream(PROPERTIES_PATH)) {
            properties.load(fileInputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        PROPERTIES = properties;

        TWITCH_TOKEN = properties.getProperty("twitch.token", null);
        TWITCH_DEFAULT_PREFIX = properties.getProperty("twitch.default_prefix", "!");
        TWITCH_DEFAULT_LOCALE_ID = properties.getProperty("twitch.default_locale_id", "en_us");

        HibernateUtil.initSessionFactory(properties);
    }

    public static final String HOLIDAY_URL = "https://hol.ilotterytea.kz/api/v1/%s/%s";
    public static final String HOLIDAY_SEARCH_URL = "https://hol.ilotterytea.kz/api/v1/search?q=%s";

    public static final String STV_EVENTAPI_ENDPOINT_URL = "wss://events.7tv.io/v3";

    public static final String STV_API_BASE_URL = "https://7tv.io/v3";
    public static final String STV_API_USER_ENDPOINT = STV_API_BASE_URL + "/users/twitch/%s";
    public static final String STV_API_SEVENTV_USER_ENDPOINT = STV_API_BASE_URL + "/users/%s";
    public static final String STV_API_EMOTESET_ENDPOINT = STV_API_BASE_URL + "/emote-sets/%s";

    public static final String STATS_URL = "https://stats.ilotterytea.kz";
    public static final String PASTEA_URL = "https://paste.ilotterytea.kz";
    public static final String SCREENSHOT_URL = "https://ttours.ilotterytea.kz";
    public static final String MARKOV_URL = "https://markov.ilotterytea.kz";

    public static final String IVR_BASE_API = "https://api.ivr.fi/v2";
    public static final String IVR_USER_ENDPOINT = IVR_BASE_API + "/twitch/user";

    public static final String MCSRVSTATUS_ENDPOINT = "https://api.mcsrvstat.us/2";
}
