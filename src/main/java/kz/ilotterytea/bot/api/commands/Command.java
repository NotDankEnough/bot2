package kz.ilotterytea.bot.api.commands;

import kz.ilotterytea.bot.entities.permissions.Permission;

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
    Response run(Request request);
}
