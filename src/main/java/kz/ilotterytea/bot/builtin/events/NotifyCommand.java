package kz.ilotterytea.bot.builtin.events;

import kz.ilotterytea.bot.Huinyabot;
import kz.ilotterytea.bot.api.commands.*;
import kz.ilotterytea.bot.entities.channels.Channel;
import kz.ilotterytea.bot.entities.events.Event;
import kz.ilotterytea.bot.entities.events.EventFlag;
import kz.ilotterytea.bot.entities.events.EventType;
import kz.ilotterytea.bot.entities.events.subscriptions.EventSubscription;
import kz.ilotterytea.bot.entities.users.User;
import kz.ilotterytea.bot.i18n.LineIds;
import kz.ilotterytea.bot.utils.ParsedMessage;
import org.hibernate.Session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Notify command.
 *
 * @author ilotterytea
 * @since 1.6
 */
public class NotifyCommand implements Command {
    @Override
    public String getNameId() {
        return "notify";
    }

    @Override
    public List<String> getSubcommands() {
        return List.of("sub", "unsub", "list", "subs");
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("n");
    }

    @Override
    public Response run(Request request) {
        ParsedMessage message = request.getMessage();
        Channel channel = request.getChannel();
        User user = request.getUser();
        Session session = request.getSession();

        if (message.getSubcommandId().isEmpty()) {
            throw CommandException.notEnoughArguments(request, CommandArgument.SUBCOMMAND);
        }

        final String subcommandId = message.getSubcommandId().get();

        if (subcommandId.equals("list")) {
            List<Event> events = channel.getEvents().stream().filter(it -> !it.getFlags().contains(EventFlag.NON_SUBSCRIPTION)).collect(Collectors.toList());

            if (events.isEmpty()) {
                return Response.ofSingle(Huinyabot.getInstance().getLocale().literalText(
                        channel.getPreferences().getLanguage(),
                        LineIds.C_NOTIFY_NOEVENTS
                ));
            }

            return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                    channel.getPreferences().getLanguage(),
                    LineIds.C_NOTIFY_LIST,
                    events.stream().map(it -> {
                        if (it.getEventType().equals(EventType.CUSTOM)) {
                            return "\"" + it.getEventName() + "\"";
                        }

                        return "\"" + it.getAliasId() + ":" + it.getEventType().getName() + "\"";
                    }).collect(Collectors.joining(","))
            ));
        }

        if (subcommandId.equals("subs")) {
            List<EventSubscription> eventSubscriptions = new ArrayList<>(user.getSubscriptions());

            if (eventSubscriptions.isEmpty()) {
                return Response.ofSingle(Huinyabot.getInstance().getLocale().literalText(
                        channel.getPreferences().getLanguage(),
                        LineIds.C_NOTIFY_NOSUBS
                ));
            }

            return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                    channel.getPreferences().getLanguage(),
                    LineIds.C_NOTIFY_SUBS,
                    eventSubscriptions.stream().map(it -> {
                        if (it.getEvent().getEventType().equals(EventType.CUSTOM)) {
                            return "\"" + it.getEvent().getEventName() + "\"";
                        }

                        return "\"" + it.getEvent().getAliasId() + ":" + it.getEvent().getEventType().getName() + "\"";
                    }).collect(Collectors.joining(","))
            ));
        }

        // Clauses that requires a message
        if (message.getMessage().isEmpty()) {
            throw CommandException.notEnoughArguments(request, CommandArgument.VALUE);
        }

        final String msg = message.getMessage().get();
        ArrayList<String> msgSplit = new ArrayList<>(List.of(msg.split(" ")));

        final String formattedEventName;
        final String eventName;
        final EventType eventType;
        String[] targetAndEvent = msgSplit.get(0).split(":");

        if (targetAndEvent.length == 2) {
            Optional<EventType> optionalEventType = EventType.findEventTypeById(targetAndEvent[1]);

            if (optionalEventType.isEmpty()) {
                eventType = EventType.CUSTOM;
                eventName = msgSplit.get(0);
                formattedEventName = msgSplit.get(0) + " [CUSTOM]";
            } else {
                eventType = optionalEventType.get();
                formattedEventName = targetAndEvent[0] + ":" + targetAndEvent[1];

                List<com.github.twitch4j.helix.domain.User> users = Huinyabot.getInstance().getClient().getHelix().getUsers(
                        Huinyabot.getInstance().getCredential().getAccessToken(),
                        null,
                        Collections.singletonList(targetAndEvent[0])
                ).execute().getUsers();

                if (users.isEmpty()) {
                    throw CommandException.notFound(request, targetAndEvent[0]);
                }

                eventName = users.get(0).getId();
            }
        } else {
            eventType = EventType.CUSTOM;
            eventName = msgSplit.get(0);
            formattedEventName = msgSplit.get(0);
        }

        msgSplit.remove(0);

        Optional<Event> optionalEvent = channel.getEvents()
                .stream()
                .filter(it -> {
                    if (eventType == EventType.CUSTOM) {
                        return it.getEventName().equals(eventName);
                    }

                    return it.getAliasId().equals(Integer.parseInt(eventName)) && it.getEventType().equals(eventType);
                })
                .findFirst();

        if (optionalEvent.isEmpty()) {
            throw CommandException.notFound(request, formattedEventName);
        }

        Event event1 = optionalEvent.get();
        Optional<EventSubscription> optionalEventSubscription = user.getSubscriptions()
                .stream()
                .filter(it -> it.getEvent().getId().equals(event1.getId()))
                .findFirst();

        if (subcommandId.equals("sub")) {
            if (optionalEventSubscription.isPresent()) {
                return Response.ofSingle(Huinyabot.getInstance().getLocale().literalText(
                        channel.getPreferences().getLanguage(),
                        LineIds.C_NOTIFY_SUBALREADY
                ));
            }

            if (event1.getFlags().contains(EventFlag.NON_SUBSCRIPTION)) {
                return Response.ofSingle(Huinyabot.getInstance().getLocale().literalText(
                        channel.getPreferences().getLanguage(),
                        LineIds.C_NOTIFY_NOTAVAILABLE
                ));
            }

            EventSubscription eventSubscription = new EventSubscription();
            user.addSubscription(eventSubscription);
            event1.addSubscription(eventSubscription);

            session.persist(eventSubscription);
            session.merge(user);
            session.merge(event1);

            return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                    channel.getPreferences().getLanguage(),
                    LineIds.C_NOTIFY_SUB,
                    formattedEventName
            ));
        }

        if (subcommandId.equals("unsub")) {
            if (optionalEventSubscription.isEmpty()) {
                return Response.ofSingle(Huinyabot.getInstance().getLocale().literalText(
                        channel.getPreferences().getLanguage(),
                        LineIds.C_NOTIFY_NOTSUBBED
                ));
            }

            session.remove(optionalEventSubscription.get());

            return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                    channel.getPreferences().getLanguage(),
                    LineIds.C_NOTIFY_UNSUB,
                    formattedEventName
            ));
        }

        return Response.ofNothing();
    }
}
