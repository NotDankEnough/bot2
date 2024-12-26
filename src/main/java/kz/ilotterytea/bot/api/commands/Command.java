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
    default int getDelay() {
        return 5000;
    }

    /**
     * Get the ID of minimal permissions to run the command.
     *
     * @return permission ID.
     */
    default Permission getPermissions() {
        return Permission.USER;
    }

    /**
     * Get the names of the options that should be used in the command.
     *
     * @return array list of option names.
     */
    default List<String> getOptions() {
        return List.of();
    }

    /**
     * Get the names of the subcommands that should be used in the command.
     *
     * @return array list of subcommand names.
     */
    default List<String> getSubcommands() {
        return List.of();
    }

    /**
     * Get command alias names.
     *
     * @return array list of alias names.
     */
    default List<String> getAliases() {
        return List.of();
    }

    /**
     * Get command required arguments.
     *
     * @return array list of required arguments.
     */
    default List<CommandArgument> getRequiredArguments() {
        return List.of();
    }

    /**
     * Run the command.
     *
     * @return response.
     * @author ilotterytea
     * @since 1.0
     */
    Response run(Request request) throws Exception;
}
