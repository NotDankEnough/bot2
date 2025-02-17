package kz.ilotterytea.bot;

import com.github.ilotterytea.emotes4j.betterttv.BetterTTVEventClient;
import com.github.ilotterytea.emotes4j.betterttv.events.EmoteCreateEvent;
import com.github.ilotterytea.emotes4j.betterttv.events.EmoteDeleteEvent;
import com.github.ilotterytea.emotes4j.betterttv.events.EmoteUpdateEvent;
import com.github.ilotterytea.emotes4j.seventv.SevenTVEventClient;
import com.github.ilotterytea.emotes4j.seventv.events.EmoteSetUpdateEvent;
import com.github.ilotterytea.emotes4j.seventv.events.HeartbeatEvent;
import com.github.ilotterytea.emotes4j.seventv.events.HelloEvent;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.github.twitch4j.events.ChannelGoOfflineEvent;
import com.github.twitch4j.helix.domain.User;
import kz.ilotterytea.bot.api.commands.CommandLoader;
import kz.ilotterytea.bot.clients.GithubListenerClient;
import kz.ilotterytea.bot.entities.channels.Channel;
import kz.ilotterytea.bot.entities.channels.ChannelFeature;
import kz.ilotterytea.bot.entities.channels.ChannelPreferences;
import kz.ilotterytea.bot.entities.events.Event;
import kz.ilotterytea.bot.handlers.EventHandlers;
import kz.ilotterytea.bot.handlers.MessageHandlerSamples;
import kz.ilotterytea.bot.handlers.StreamEventHandlers;
import kz.ilotterytea.bot.i18n.I18N;
import kz.ilotterytea.bot.utils.HibernateUtil;
import kz.ilotterytea.bot.utils.StorageUtils;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Bot.
 *
 * @author ilotterytea
 * @since 1.0
 */
public class Huinyabot extends Bot {
    private TwitchClient client;
    private CommandLoader loader;
    private SevenTVEventClient stvEventClient;
    private BetterTTVEventClient betterTTVEventClient;
    private OAuth2Credential credential;
    private I18N i18N;

    private final Logger log = LoggerFactory.getLogger(Huinyabot.class);

    public TwitchClient getClient() {
        return client;
    }

    public CommandLoader getLoader() {
        return loader;
    }

    public OAuth2Credential getCredential() {
        return credential;
    }

    public I18N getLocale() {
        return i18N;
    }

    private static Huinyabot instance;

    public static Huinyabot getInstance() {
        return instance;
    }

    public Huinyabot() {
        instance = this;
    }

    @Override
    public void init() {
        if (SharedConstants.TWITCH_TOKEN == null) {
            throw new RuntimeException("twitch.token must be set in config.properties!");
        }

        if (SharedConstants.TWITCH_TOKEN.startsWith("oauth:")) {
            throw new RuntimeException("twitch.token must not start with the prefix 'oauth:'!");
        }

        loader = new CommandLoader();
        i18N = new I18N(StorageUtils.getFilepathsFromResource("/i18n"));

        // - - -  T W I T C H  C L I E N T  - - - :
        credential = new OAuth2Credential("twitch", "oauth:" + SharedConstants.TWITCH_TOKEN);

        client = TwitchClientBuilder.builder()
                .withChatAccount(credential)
                .withDefaultAuthToken(credential)
                .withEnableTMI(true)
                .withEnableChat(true)
                .withEnableHelix(true)
                .build();

        client.getChat().connect();

        Session session = HibernateUtil.getSessionFactory().openSession();

        // Join bot's chat:
        if (credential.getUserName() != null && credential.getUserId() != null) {
            client.getChat().joinChannel(credential.getUserName());
            log.debug("Joined to bot's chat room!");

            // Generate a new channel for bot if it doesn't exist:
            List<Channel> channels = session.createQuery("from Channel where aliasId = :aliasId", Channel.class)
                    .setParameter("aliasId", credential.getUserId())
                    .getResultList();

            if (channels.isEmpty()) {
                log.debug("The bot doesn't have a channel entry. Creating a new one...");

                Channel channel = new Channel(Integer.parseInt(credential.getUserId()), credential.getUserName());
                ChannelPreferences preferences = new ChannelPreferences(channel);
                channel.setPreferences(preferences);

                session.getTransaction().begin();
                session.persist(channel);
                session.persist(preferences);
                session.getTransaction().commit();
            }
        }

        // Obtaining the channels:
        List<Channel> channels = session.createQuery("from Channel where optOutTimestamp is null", Channel.class).getResultList();

        if (!channels.isEmpty()) {
            List<User> twitchChannels = client.getHelix().getUsers(
                    credential.getAccessToken(),
                    channels.stream().map(c -> c.getAliasId().toString()).collect(Collectors.toList()),
                    null
            ).execute().getUsers();

            // Join channel chats:
            for (User twitchChannel : twitchChannels) {
                client.getChat().joinChannel(twitchChannel.getLogin());
                log.debug("Joined to " + twitchChannel.getLogin() + "'s chat room!");
            }
        }

        // Obtaining to stream events:
        List<Event> streamEvents = session.createQuery("from Event where eventType = LIVE or eventType = OFFLINE", Event.class).getResultList();

        if (!streamEvents.isEmpty()) {
            Set<Integer> eventIds = new HashSet<>();

            for (Event streamEvent : streamEvents) {
                eventIds.add(streamEvent.getTargetAliasId());
            }

            // Getting Twitch info about the stream events:
            List<User> listenableUsers = client.getHelix().getUsers(
                    credential.getAccessToken(),
                    eventIds.stream().map(Object::toString).collect(Collectors.toList()),
                    null
            ).execute().getUsers();

            // Listening to stream events:
            for (User listenableUser : listenableUsers) {
                client.getClientHelper().enableStreamEventListener(listenableUser.getId(), listenableUser.getLogin());
                log.debug("Listening for stream events for user " + listenableUser.getLogin());
            }
        }

        session.close();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Session session1 = HibernateUtil.getSessionFactory().openSession();
                final Date CURRENT_DATE = new Date();

                List<kz.ilotterytea.bot.entities.Timer> timers = session1.createQuery("from Timer", kz.ilotterytea.bot.entities.Timer.class).getResultList();

                session1.getTransaction().begin();

                for (kz.ilotterytea.bot.entities.Timer timer : timers) {
                    if (timer.getChannel().getPreferences().getFeatures().contains(ChannelFeature.SILENT_MODE)) {
                        continue;
                    }

                    if (CURRENT_DATE.getTime() - timer.getLastTimeExecuted().getTime() > timer.getIntervalMilliseconds()) {
                        client.getChat().sendMessage(
                                timer.getChannel().getAliasName(),
                                timer.getMessage()
                        );

                        timer.setLastTimeExecuted(new Date());
                        session1.persist(timer);
                    }
                }

                session1.getTransaction().commit();
                session1.close();
            }
        }, 2500, 2500);

        GithubListenerClient githubListenerClient = new GithubListenerClient();
        githubListenerClient.run();

        client.getEventManager().onEvent(ChannelMessageEvent.class, MessageHandlerSamples::channelMessageEvent);

        // Handling stream events:
        client.getEventManager().onEvent(ChannelGoLiveEvent.class, StreamEventHandlers::handleGoLiveEvent);
        client.getEventManager().onEvent(ChannelGoOfflineEvent.class, StreamEventHandlers::handleGoOfflineEvent);

        // Setting up 7TV EventAPI
        try {
            log.info("Connecting to 7TV Events...");
            stvEventClient = new SevenTVEventClient();
            stvEventClient.getClient().connectBlocking();
            stvEventClient.getEventManager().onEvent(EmoteSetUpdateEvent.class, EventHandlers::handleEmoteSetUpdate);
            stvEventClient.getEventManager().onEvent(HeartbeatEvent.class, EventHandlers::handleHeartbeat);
            stvEventClient.getEventManager().onEvent(HelloEvent.class, EventHandlers::handleHello);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Setting up BetterTTV Events
        try {
            log.info("Connecting to BetterTTV Events...");
            betterTTVEventClient = new BetterTTVEventClient();
            betterTTVEventClient.getClient().connectBlocking();
            betterTTVEventClient.getEventManager().onEvent(EmoteCreateEvent.class, EventHandlers::handleEmoteCreation);
            betterTTVEventClient.getEventManager().onEvent(EmoteUpdateEvent.class, EventHandlers::handleEmoteUpdate);
            betterTTVEventClient.getEventManager().onEvent(EmoteDeleteEvent.class, EventHandlers::handleEmoteDeletion);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Update BetterTTV subscribers every minute
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                // in case if SOMETHING HAPPENS (nothing ever happens)
                try {
                    EventHandlers.subscribeChannels(betterTTVEventClient, ChannelFeature.NOTIFY_BTTV);
                } catch (Exception e) {
                    log.error("An exception was thrown while checking new BetterTTV subscribers", e);
                }
            }
        }, 0, 60000);
    }

    @Override
    public void dispose() {
        client.close();
        stvEventClient.getClient().close();
    }

    public SevenTVEventClient getSevenTVEventClient() {
        return stvEventClient;
    }

    public BetterTTVEventClient getBetterTTVEventClient() {
        return betterTTVEventClient;
    }
}
