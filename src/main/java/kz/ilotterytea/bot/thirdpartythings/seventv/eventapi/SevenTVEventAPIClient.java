package kz.ilotterytea.bot.thirdpartythings.seventv.eventapi;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kz.ilotterytea.bot.Huinyabot;
import kz.ilotterytea.bot.SharedConstants;
import kz.ilotterytea.bot.entities.channels.Channel;
import kz.ilotterytea.bot.entities.channels.ChannelFeature;
import kz.ilotterytea.bot.i18n.LineIds;
import kz.ilotterytea.bot.thirdpartythings.seventv.api.SevenTVAPIClient;
import kz.ilotterytea.bot.thirdpartythings.seventv.api.schemas.User;
import kz.ilotterytea.bot.utils.HibernateUtil;
import org.hibernate.Session;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * @author ilotterytea
 * @since 1.0
 */
public class SevenTVEventAPIClient extends WebSocketClient {
    private final Logger log;
    private final String prefix;
    private final HashMap<String, String> subscriptions;

    private int heartbeatCounter, retryCounter;

    private static SevenTVEventAPIClient instance;

    public static SevenTVEventAPIClient getInstance() throws URISyntaxException {
        if (instance == null) instance = new SevenTVEventAPIClient();
        return instance;
    }

    public SevenTVEventAPIClient() throws URISyntaxException {
        super(new URI(SharedConstants.STV_EVENTAPI_ENDPOINT_URL));
        this.log = LoggerFactory.getLogger(SevenTVEventAPIClient.class.getName());
        this.prefix = "7TV";
        this.subscriptions = new HashMap<>();
        this.heartbeatCounter = 0;
        this.retryCounter = 0;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        log.info("Connected to 7TV EventAPI: {} {}", handshakedata.getHttpStatus(), handshakedata.getHttpStatusMessage());
    }

    @Override
    public void onMessage(String message) {
        JsonObject json = JsonParser.parseString(message).getAsJsonObject();
        int opCode = json.get("op").getAsInt();

        try {
            switch (opCode) {
                case 0 -> handleDispatchEvent(json.getAsJsonObject("d"));
                case 1 -> handleHelloEvent();
                case 2 -> handleHeartbeat();
            }
        } catch (Exception e) {
            log.error("An exception occurred while processing a 7TV event", e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.debug("Connection with 7TV EventAPI has been closed! Reason: {} {}.", code, reason);
        handleCloseEvent(code, reason);
    }

    @Override
    public void onError(Exception ex) {
        throw new RuntimeException(ex);
    }

    private void handleDispatchEvent(JsonObject data) {
        if (data.get("type").getAsString().equals("emote_set.update")) {
            handleEmoteSetUpdate(data.getAsJsonObject("body"));
        } else {
            log.warn("\"emote_set.update\" events are only supported for now.");
        }
    }

    private void handleEmoteSetUpdate(JsonObject body) {
        // Getting source
        String emoteSetId = body.get("id").getAsString();
        String aliasId = null;

        for (Map.Entry<String, String> entry : this.subscriptions.entrySet()) {
            if (entry.getValue().equals(emoteSetId)) {
                aliasId = entry.getKey();
                break;
            }
        }

        if (aliasId == null) {
            log.warn("Received an event for emote set ID {}, but cannot find relative alias ID", emoteSetId);
            return;
        }

        Session session = HibernateUtil.getSessionFactory().openSession();
        Channel channel = session.createQuery("from Channel where aliasId = :aliasId AND optOutTimestamp is null", Channel.class)
                .setParameter("aliasId", aliasId)
                .getSingleResult();
        session.close();

        if (channel.getPreferences().getFeatures().contains(ChannelFeature.SILENT_MODE)) {
            log.warn("Received an event for emote set ID {}, but the channel ID {} is in silent mode", emoteSetId, channel.getId());
            return;
        }

        // Getting the actor
        String actorName = body.getAsJsonObject("actor").get("username").getAsString();

        // Getting the emotes
        Huinyabot bot = Huinyabot.getInstance();
        ArrayList<String> messages = new ArrayList<>();

        if (body.has("pulled")) {
            for (JsonElement element : body.getAsJsonArray("pulled").asList()) {
                JsonObject emote = element.getAsJsonObject();
                String oldName = emote.getAsJsonObject("old_value").get("name").getAsString();
                messages.add(bot.getLocale()
                        .formattedText(channel.getPreferences().getLanguage(),
                                LineIds.REMOVED_EMOTE_WITH_AUTHOR,
                                this.prefix,
                                actorName,
                                oldName
                        )
                );
            }
        }

        if (body.has("pushed")) {
            for (JsonElement element : body.getAsJsonArray("pushed").asList()) {
                JsonObject emote = element.getAsJsonObject();
                String name = emote.getAsJsonObject("value").get("name").getAsString();
                messages.add(bot.getLocale()
                        .formattedText(channel.getPreferences().getLanguage(),
                                LineIds.NEW_EMOTE_WITH_AUTHOR,
                                this.prefix,
                                actorName,
                                name
                        )
                );
            }
        }

        if (body.has("updated")) {
            for (JsonElement element : body.getAsJsonArray("updated").asList()) {
                JsonObject emote = element.getAsJsonObject();
                String oldName = emote.getAsJsonObject("old_value").get("name").getAsString();
                String name = emote.getAsJsonObject("value").get("name").getAsString();
                messages.add(bot.getLocale()
                        .formattedText(channel.getPreferences().getLanguage(),
                                LineIds.UPDATED_EMOTE_WITH_AUTHOR,
                                this.prefix,
                                actorName,
                                oldName,
                                name
                        )
                );
            }
        }

        // Sending notifications
        for (String message : messages) {
            bot.getClient().getChat().sendMessage(channel.getAliasName(), message);
        }
    }

    private void handleHelloEvent() {
        log.info("7TV EventAPI has greeted me. Starting to subscribe...");
        subscribeToChannels();
    }

    private void handleHeartbeat() {
        heartbeatCounter++;

        // Check subscriptions every 3 heartbeats
        if (heartbeatCounter % 3 == 0) subscribeToChannels();
    }

    private void handleCloseEvent(int code, String message) {
        log.info("Closing the 7TV EventAPI connection... Reason: {} {}", code, message);

        if (retryCounter != 0) {
            log.info("The reconnection task wasn't run because the retry counter wasn't reset to zero.");
            return;
        }

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                retryCounter++;

                if (retryCounter > 4) {
                    log.info("Retry limit has been exceeded! Cancelling the reconnection task...");
                    retryCounter = 0;
                    super.cancel();
                    return;
                }

                log.info("Reconnecting to 7TV EventAPI...");

                try {
                    if (reconnectBlocking()) {
                        log.info("Successfully reconnected to 7TV EventAPI!");
                        retryCounter = 0;
                        heartbeatCounter = 0;
                        subscriptions.clear();
                        super.cancel();
                    } else {
                        log.info("Failed to reconnect to 7TV EventAPI! Retrying...");
                    }
                } catch (Exception e) {
                    log.error("An exception occurred while reconnecting to 7TV", e);
                }
            }
        }, 300000, 300000);
    }

    private void subscribeToChannels() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        List<Channel> channels = session.createQuery("from Channel where optOutTimestamp is null", Channel.class).getResultList();
        session.close();

        channels = channels.stream().filter((x) -> {
            Set<ChannelFeature> features = x.getPreferences().getFeatures();
            return features.contains(ChannelFeature.NOTIFY_7TV) && !features.contains(ChannelFeature.SILENT_MODE);
        }).toList();

        // Adding new channels
        for (Channel channel : channels) {
            if (subscribeEmoteSet(channel.getAliasId().toString())) {
                log.info("Subscribing to channel alias ID {} events...", channel.getAliasId());
            }
        }

        // Removing old channels
        for (String userId : this.subscriptions.keySet()) {
            if (channels.stream().anyMatch((x) -> x.getAliasId().toString().equals(userId))) {
                continue;
            }

            if (unSubscribeEmoteSet(userId)) {
                log.info("Unsubscribing from alias ID {} events...", userId);
            }
        }
    }

    private boolean subscribeEmoteSet(String userId) {
        if (this.subscriptions.containsKey(userId)) return false;
        User user = SevenTVAPIClient.getUser(userId);

        if (user == null) return false;

        this.send(String.format("""
                {
                    "op": 35,
                    "d": {
                        "type": "emote_set.update",
                        "condition": {
                            "object_id": "%s"
                        }
                    }
                }""", user.getEmoteSetId()));

        this.subscriptions.put(userId, user.getEmoteSetId());
        return true;
    }

    private boolean unSubscribeEmoteSet(String userId) {
        if (!this.subscriptions.containsKey(userId)) return false;
        User user = SevenTVAPIClient.getUser(userId);

        if (user == null) return false;

        this.send(String.format("""
                {
                    "op": 36,
                    "d": {
                        "type": "emote_set.update",
                        "condition": {
                            "object_id": "%s"
                        }
                    }
                }""", user.getEmoteSetId()));

        this.subscriptions.remove(userId);
        return true;
    }
}
