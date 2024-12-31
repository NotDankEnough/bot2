package kz.ilotterytea.bot.builtin.channel;

import kz.ilotterytea.bot.Huinyabot;
import kz.ilotterytea.bot.api.commands.*;
import kz.ilotterytea.bot.entities.Timer;
import kz.ilotterytea.bot.entities.channels.Channel;
import kz.ilotterytea.bot.entities.permissions.Permission;
import kz.ilotterytea.bot.i18n.LineIds;
import kz.ilotterytea.bot.utils.ParsedMessage;
import org.hibernate.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Timer command.
 *
 * @author ilotterytea
 * @since 1.5
 */
public class TimerCommand implements Command {
    @Override
    public String getNameId() {
        return "timer";
    }

    @Override
    public Permission getPermissions() {
        return Permission.MOD;
    }

    @Override
    public List<String> getSubcommands() {
        return List.of("new", "delete", "message", "interval", "list", "info");
    }

    @Override
    public Response run(Request request) {
        ParsedMessage message = request.getMessage();
        Channel channel = request.getChannel();
        Session session = request.getSession();

        if (message.getSubcommandId().isEmpty()) {
            throw CommandException.notEnoughArguments(request, CommandArgument.SUBCOMMAND);
        }

        List<Timer> timers = session.createQuery("from Timer where channel = :channel", Timer.class)
                .setParameter("channel", channel)
                .getResultList();

        if (message.getSubcommandId().get().equals("list")) {
            return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                    channel.getPreferences().getLanguage(),
                    LineIds.C_TIMER_LIST,
                    channel.getAliasName(),
                    timers.stream().map(Timer::getName).collect(Collectors.joining(", "))
            ));
        }

        if (message.getMessage().isEmpty()) {
            throw CommandException.notEnoughArguments(request, CommandArgument.VALUE);
        }

        ArrayList<String> s = new ArrayList<>(List.of(message.getMessage().get().split(" ")));

        String timerId = s.get(0);
        s.remove(0);

        if (message.getSubcommandId().get().equals("new")) {
            if (timers.stream().anyMatch(t -> t.getName().equals(timerId))) {
                throw CommandException.namesakeCreation(request, timerId);
            }

            if (s.isEmpty() || s.size() < 2) {
                throw CommandException.notEnoughArguments(request, CommandArgument.MESSAGE);
            }

            int intervalMs;

            try {
                intervalMs = Integer.parseInt(s.get(0));
                s.remove(0);
            } catch (NumberFormatException e) {
                throw CommandException.incorrectArgument(request, CommandArgument.INTERVAL, s.get(0));
            }

            Timer timer = new Timer(channel, timerId, String.join(" ", s), intervalMs);
            channel.addTimer(timer);

            session.persist(timer);
            session.merge(channel);

            return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                    channel.getPreferences().getLanguage(),
                    LineIds.C_TIMER_NEW,
                    timerId
            ));
        }

        if (timers.stream().noneMatch(t -> t.getName().equals(timerId))) {
            throw CommandException.notFound(request, timerId);
        }

        Timer timer = timers.stream().filter(t -> t.getName().equals(timerId)).findFirst().get();

        switch (message.getSubcommandId().get()) {
            case "delete":
                session.remove(timer);

                return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                        channel.getPreferences().getLanguage(),
                        LineIds.C_TIMER_DELETE,
                        timerId
                ));
            case "info":
                return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                        channel.getPreferences().getLanguage(),
                        LineIds.C_TIMER_INFO,
                        timerId,
                        String.valueOf(timer.getIntervalMilliseconds()),
                        timer.getMessage()
                ));
            default:
                break;
        }

        if (s.isEmpty()) {
            throw CommandException.notEnoughArguments(request, CommandArgument.MESSAGE);
        }

        String msg = String.join(" ", s);

        switch (message.getSubcommandId().get()) {
            case "message":
                timer.setMessage(msg);

                session.persist(timer);

                return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                        channel.getPreferences().getLanguage(),
                        LineIds.C_TIMER_MESSAGE,
                        timerId
                ));
            case "interval":
                int interval;

                try {
                    interval = Integer.parseInt(msg);
                } catch (NumberFormatException e) {
                    throw CommandException.incorrectArgument(request, CommandArgument.INTERVAL, msg);
                }

                timer.setIntervalMilliseconds(interval);

                session.persist(timer);

                return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                        channel.getPreferences().getLanguage(),
                        LineIds.C_TIMER_INTERVAL,
                        timerId
                ));
            default:
                break;
        }

        return Response.ofNothing();
    }
}
