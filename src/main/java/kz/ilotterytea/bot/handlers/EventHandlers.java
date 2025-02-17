package kz.ilotterytea.bot.handlers;

import com.github.ilotterytea.emotes4j.betterttv.BetterTTVEventClient;
import com.github.ilotterytea.emotes4j.betterttv.events.EmoteCreateEvent;
import com.github.ilotterytea.emotes4j.betterttv.events.EmoteDeleteEvent;
import com.github.ilotterytea.emotes4j.betterttv.events.EmoteUpdateEvent;
import com.github.ilotterytea.emotes4j.core.EventClient;
import com.github.ilotterytea.emotes4j.seventv.SevenTVEventClient;
import com.github.ilotterytea.emotes4j.seventv.emotes.EventEmote;
import com.github.ilotterytea.emotes4j.seventv.events.EmoteSetUpdateEvent;
import com.github.ilotterytea.emotes4j.seventv.events.HeartbeatEvent;
import com.github.ilotterytea.emotes4j.seventv.events.HelloEvent;
import kz.ilotterytea.bot.Huinyabot;
import kz.ilotterytea.bot.entities.channels.Channel;
import kz.ilotterytea.bot.entities.channels.ChannelFeature;
import kz.ilotterytea.bot.i18n.LineIds;
import kz.ilotterytea.bot.utils.HibernateUtil;
import org.hibernate.Session;

import java.util.*;

public class EventHandlers {
    public static void handleEmoteSetUpdate(EmoteSetUpdateEvent event) {
        Optional<Channel> channelOptional = getChannelBySTV(Huinyabot.getInstance().getSevenTVEventClient(), event.getEmoteSetId());

        if (channelOptional.isEmpty()) return;
        Channel channel = channelOptional.get();

        String prefix = "7TV";
        Huinyabot bot = Huinyabot.getInstance();
        ArrayList<String> messages = new ArrayList<>();

        if (event.getPulled() != null) {
            for (EventEmote emote : event.getPulled()) {
                if (emote.getOldValue() == null) continue;
                messages.add(bot.getLocale()
                        .formattedText(channel.getPreferences().getLanguage(),
                                LineIds.REMOVED_EMOTE_WITH_AUTHOR,
                                prefix,
                                event.getActor().getUsername(),
                                emote.getOldValue().getName())
                );
            }
        }

        if (event.getPushed() != null) {
            for (EventEmote emote : event.getPushed()) {
                if (emote.getValue() == null) continue;
                messages.add(bot.getLocale()
                        .formattedText(channel.getPreferences().getLanguage(),
                                LineIds.NEW_EMOTE_WITH_AUTHOR,
                                prefix,
                                event.getActor().getUsername(),
                                emote.getValue().getName())
                );
            }
        }

        if (event.getUpdated() != null) {
            for (EventEmote emote : event.getUpdated()) {
                if (emote.getOldValue() == null || emote.getValue() == null) continue;
                messages.add(bot.getLocale()
                        .formattedText(channel.getPreferences().getLanguage(),
                                LineIds.UPDATED_EMOTE_WITH_AUTHOR,
                                prefix,
                                event.getActor().getUsername(),
                                emote.getOldValue().getName(),
                                emote.getValue().getName())
                );
            }
        }

        for (String message : messages) {
            Huinyabot.getInstance().getClient().getChat().sendMessage(
                    channel.getAliasName(),
                    message
            );
        }
    }

    public static void handleHeartbeat(HeartbeatEvent event) {
        subscribeChannels(Huinyabot.getInstance().getSevenTVEventClient(), ChannelFeature.NOTIFY_7TV);
    }

    public static void handleHello(HelloEvent event) {
        subscribeChannels(Huinyabot.getInstance().getSevenTVEventClient(), ChannelFeature.NOTIFY_7TV);
    }

    public static void handleEmoteCreation(EmoteCreateEvent event) {
        Optional<Channel> channelOptional = getChannelByBTTV(Huinyabot.getInstance().getBetterTTVEventClient(), event.getChannel());

        if (channelOptional.isEmpty()) return;
        Channel channel = channelOptional.get();

        String prefix = "BTTV";
        Huinyabot bot = Huinyabot.getInstance();

        String line = bot.getLocale().formattedText(channel.getPreferences().getLanguage(),
                LineIds.NEW_EMOTE,
                prefix,
                event.getEmote().getCode()
        );

        bot.getClient().getChat().sendMessage(channel.getAliasName(), line);
    }

    public static void handleEmoteUpdate(EmoteUpdateEvent event) {
        Optional<Channel> channelOptional = getChannelByBTTV(Huinyabot.getInstance().getBetterTTVEventClient(), event.getChannel());

        if (channelOptional.isEmpty()) return;
        Channel channel = channelOptional.get();

        String prefix = "BTTV";
        Huinyabot bot = Huinyabot.getInstance();

        String line = bot.getLocale().formattedText(channel.getPreferences().getLanguage(),
                LineIds.UPDATED_EMOTE,
                prefix,
                event.getOldEmote().getCode(),
                event.getEmote().getCode()
        );

        bot.getClient().getChat().sendMessage(channel.getAliasName(), line);
    }

    public static void handleEmoteDeletion(EmoteDeleteEvent event) {
        Optional<Channel> channelOptional = getChannelByBTTV(Huinyabot.getInstance().getBetterTTVEventClient(), event.getChannel());

        if (channelOptional.isEmpty()) return;
        Channel channel = channelOptional.get();

        String prefix = "BTTV";
        Huinyabot bot = Huinyabot.getInstance();

        String line = bot.getLocale().formattedText(channel.getPreferences().getLanguage(),
                LineIds.REMOVED_EMOTE,
                prefix,
                event.getEmote().getCode()
        );

        bot.getClient().getChat().sendMessage(channel.getAliasName(), line);
    }

    public static void subscribeChannels(EventClient<?> eventClient, ChannelFeature feature) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        List<Channel> channels = session.createQuery("from Channel where optOutTimestamp is null", Channel.class).getResultList();
        session.close();

        channels = channels.stream().filter((x) -> {
            Set<ChannelFeature> features = x.getPreferences().getFeatures();
            return features.contains(feature) && !features.contains(ChannelFeature.SILENT_MODE);
        }).toList();

        // Adding new channels
        for (Channel channel : channels) {
            eventClient.subscribeChannel(channel.getAliasId().toString());
        }

        // Removing old channels
        for (String userId : eventClient.getSubscriptions().keySet()) {
            if (channels.stream().anyMatch((x) -> x.getAliasId().toString().equals(userId))) {
                continue;
            }

            eventClient.unsubscribeChannel(userId);
        }
    }

    private static Optional<Channel> getChannelByBTTV(BetterTTVEventClient client, String id) {
        String aliasId = null;

        if (client.getSubscriptions().containsKey(id)) {
            aliasId = id;
        }

        if (aliasId == null) {
            return Optional.empty();
        }

        aliasId = aliasId.substring("twitch:".length());

        Session session = HibernateUtil.getSessionFactory().openSession();
        Channel channel = session.createQuery("from Channel where aliasId = :aliasId AND optOutTimestamp is null", Channel.class)
                .setParameter("aliasId", aliasId)
                .getSingleResult();
        session.close();

        if (channel.getPreferences().getFeatures().contains(ChannelFeature.SILENT_MODE) || !channel.getPreferences().getFeatures().contains(ChannelFeature.NOTIFY_BTTV)) {
            return Optional.empty();
        }

        return Optional.of(channel);
    }

    private static Optional<Channel> getChannelBySTV(SevenTVEventClient client, String id) {
        String aliasId = null;

        for (Map.Entry<String, String> entry : client.getSubscriptions().entrySet()) {
            if (entry.getValue().equals(id)) {
                aliasId = entry.getKey();
                break;
            }
        }

        if (aliasId == null) {
            return Optional.empty();
        }

        Session session = HibernateUtil.getSessionFactory().openSession();
        Channel channel = session.createQuery("from Channel where aliasId = :aliasId AND optOutTimestamp is null", Channel.class)
                .setParameter("aliasId", aliasId)
                .getSingleResult();
        session.close();

        if (channel.getPreferences().getFeatures().contains(ChannelFeature.SILENT_MODE) || !channel.getPreferences().getFeatures().contains(ChannelFeature.NOTIFY_7TV)) {
            return Optional.empty();
        }

        return Optional.of(channel);
    }
}
