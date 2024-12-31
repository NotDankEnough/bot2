package kz.ilotterytea.bot.builtin.channel;

import kz.ilotterytea.bot.Huinyabot;
import kz.ilotterytea.bot.api.commands.*;
import kz.ilotterytea.bot.entities.CustomCommand;
import kz.ilotterytea.bot.entities.channels.Channel;
import kz.ilotterytea.bot.entities.permissions.Permission;
import kz.ilotterytea.bot.i18n.LineIds;
import kz.ilotterytea.bot.utils.ParsedMessage;
import org.hibernate.Session;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 'Custom command control' command.
 *
 * @author ilotterytea
 * @since 1.1
 */
public class CustomCommandControl implements Command {
    @Override
    public String getNameId() {
        return "cmd";
    }

    @Override
    public List<String> getOptions() {
        return Collections.singletonList("no-mention");
    }

    @Override
    public List<String> getSubcommands() {
        return List.of("new", "edit", "delete", "rename", "copy", "toggle", "list");
    }

    @Override
    public List<String> getAliases() {
        return List.of("scmd", "custom", "command", "команда");
    }

    @Override
    public Response run(Request request) throws CommandException {
        ParsedMessage message = request.getMessage();
        Channel channel = request.getChannel();
        Session session = request.getSession();

        if (message.getMessage().isEmpty()) {
            throw CommandException.notEnoughArguments(request, CommandArgument.MESSAGE);
        }

        ArrayList<String> s = new ArrayList<>(List.of(message.getMessage().get().split(" ")));

        if (message.getSubcommandId().isEmpty()) {
            throw CommandException.notEnoughArguments(request, CommandArgument.SUBCOMMAND);
        }

        if (message.getSubcommandId().get().equals("list")) {
            if (channel.getCommands().isEmpty()) {
                return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                        channel.getPreferences().getLanguage(),
                        LineIds.C_CMD_NOCMDS,
                        channel.getAliasName()
                ));
            }

            return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                    channel.getPreferences().getLanguage(),
                    LineIds.C_CMD_SUCCESS_LIST,
                    channel.getAliasName(),
                    channel.getCommands().stream().map(CustomCommand::getName).collect(Collectors.joining(", "))
            ));
        }

        if (Objects.equals(s.get(0), "")) {
            throw CommandException.notEnoughArguments(request, CommandArgument.VALUE);
        }


        final String name = s.get(0);
        s.remove(0);

        // If the command was run by a broadcaster:
        if (request.getUserPermission().getLevel().getValue() >= Permission.BROADCASTER.getValue()) {
            Optional<CustomCommand> optionalCustomCommands = channel.getCommands().stream().filter(c -> c.getName().equals(name)).findFirst();
            String response = String.join(" ", s);

            if (Objects.equals(response, "")) {
                throw CommandException.notEnoughArguments(request, CommandArgument.VALUE);
            }

            // Create a new custom command:
            if (message.getSubcommandId().get().equals("new")) {
                // Check if a command with the same name already exists:
                if (optionalCustomCommands.isPresent() || Huinyabot.getInstance().getLoader().getCommand(name, request.getChannel().getPreferences().getPrefix()).isPresent()) {
                    throw CommandException.namesakeCreation(request, name);
                }

                // Creating a new command and assign it to the channel:
                CustomCommand command = new CustomCommand(name, response, channel);
                channel.addCommand(command);

                // Saving changes:
                session.persist(channel);
                session.persist(command);

                return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                        channel.getPreferences().getLanguage(),
                        LineIds.C_CMD_SUCCESS_NEW,
                        command.getName()
                ));
            }

            // If the command not exists:
            if (optionalCustomCommands.isEmpty()) {
                throw CommandException.notFound(request, name);
            }

            CustomCommand command = optionalCustomCommands.get();

            switch (message.getSubcommandId().get()) {
                // "Edit a command response" clause:
                case "edit": {
                    // Setting a new response:
                    command.setMessage(response);

                    // Saving changes:
                    session.persist(command);

                    return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                            channel.getPreferences().getLanguage(),
                            LineIds.C_CMD_SUCCESS_EDIT,
                            command.getName()
                    ));
                }
                // "Delete a command" clause:
                case "delete":
                    // Deleting a command and saving changes:
                    session.remove(command);

                    return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                            channel.getPreferences().getLanguage(),
                            LineIds.C_CMD_SUCCESS_DELETE,
                            command.getName()
                    ));
                case "rename":
                    String nameToRename = s.get(0);
                    String previousName = command.getName();

                    System.out.println(nameToRename);
                    System.out.println(previousName);

                    command.setName(nameToRename);

                    // Saving changes:
                    session.persist(command);

                    return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                            channel.getPreferences().getLanguage(),
                            LineIds.C_CMD_SUCCESS_RENAME,
                            previousName,
                            nameToRename
                    ));
                default:
                    break;
            }
        }

        return Response.ofNothing();
    }
}
