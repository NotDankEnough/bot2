package kz.ilotterytea.bot.builtin.channel;

import kz.ilotterytea.bot.Huinyabot;
import kz.ilotterytea.bot.api.commands.*;
import kz.ilotterytea.bot.entities.channels.Channel;
import kz.ilotterytea.bot.entities.channels.ChannelPreferences;
import kz.ilotterytea.bot.entities.permissions.Permission;
import kz.ilotterytea.bot.i18n.LineIds;
import kz.ilotterytea.bot.utils.ParsedMessage;
import org.hibernate.Session;

import java.util.List;

/**
 * Ping command.
 *
 * @author ilotterytea
 * @since 1.3
 */
public class SetterCommand implements Command {
    @Override
    public String getNameId() {
        return "set";
    }

    @Override
    public Permission getPermissions() {
        return Permission.BROADCASTER;
    }

    @Override
    public List<String> getSubcommands() {
        return List.of("prefix", "locale");
    }

    @Override
    public Response run(Request request) throws CommandException {
        ParsedMessage message = request.getMessage();
        Channel channel = request.getChannel();
        Session session = request.getSession();

        if (message.getSubcommandId().isEmpty()) {
            throw CommandException.notEnoughArguments(request, CommandArgument.SUBCOMMAND);
        }

        if (message.getMessage().isEmpty()) {
            throw CommandException.notEnoughArguments(request, CommandArgument.MESSAGE);
        }

        switch (message.getSubcommandId().get()) {
            // "Prefix" clause.
            case "prefix": {
                ChannelPreferences preferences = channel.getPreferences();
                preferences.setPrefix(message.getMessage().get());

                session.merge(preferences);

                return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                        preferences.getLanguage(),
                        LineIds.C_SET_SUCCESS_PREFIX_SET,
                        preferences.getPrefix()
                ));
            }
            // "Locale", "language" clause.
            case "locale":
                if (!Huinyabot.getInstance().getLocale().getLocaleIds().contains(message.getMessage().get().toLowerCase())) {
                    return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                            channel.getPreferences().getLanguage(),
                            LineIds.C_SET_SUCCESS_LOCALE_LIST,
                            String.join(", ", Huinyabot.getInstance().getLocale().getLocaleIds())
                    ));
                }

                ChannelPreferences preferences = channel.getPreferences();
                preferences.setLanguage(message.getMessage().get().toLowerCase());

                session.merge(preferences);

                return Response.ofSingle(Huinyabot.getInstance().getLocale().literalText(
                        preferences.getLanguage(),
                        LineIds.C_SET_SUCCESS_LOCALE_SET
                ));
            default:
                throw CommandException.somethingWentWrong(request);
        }
    }
}
