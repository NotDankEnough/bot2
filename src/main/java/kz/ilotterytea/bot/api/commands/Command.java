package kz.ilotterytea.bot.api.commands;

import com.github.twitch4j.chat.events.channel.IRCMessageEvent;
import kz.ilotterytea.bot.entities.channels.Channel;
import kz.ilotterytea.bot.entities.permissions.Permission;
import kz.ilotterytea.bot.entities.permissions.UserPermission;
import kz.ilotterytea.bot.entities.users.User;
import kz.ilotterytea.bot.utils.ParsedMessage;
import org.hibernate.Session;

import java.util.List;

/**
 * Command.
 *
 * @author ilotterytea
 * @since 1.0
 */
public interface Command {
    /**
     * Get the name ID of command.
     *
     * @return name ID.
     * @author ilotterytea
     * @since 1.0
     */
    String getNameId();

    /**
     * Get the seconds delay between command executions.
     *
     * @return delay.
     * @author ilotterytea
     * @since 1.0
     */
    int getDelay();

    /**
     * Get the ID of minimal permissions to run the command.
     *
     * @return permission ID.
     */
    Permission getPermissions();

    /**
     * Get the names of the options that should be used in the command.
     *
     * @return array list of option names.
     */
    List<String> getOptions();

    /**
     * Get the names of the subcommands that should be used in the command.
     *
     * @return array list of subcommand names.
     */
    List<String> getSubcommands();

    /**
     * Get command alias names.
     *
     * @return array list of alias names.
     */
    List<String> getAliases();

    /**
     * Run the command.
     *
     * @return response.
     * @author ilotterytea
     * @since 1.0
     */
    Response run(
            Session session,
            IRCMessageEvent event,
            ParsedMessage message,
            Channel channel,
            User user,
            UserPermission permission
    );
}
