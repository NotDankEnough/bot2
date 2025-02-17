package kz.ilotterytea.bot.api.commands;

import kz.ilotterytea.bot.entities.Action;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Command loader.
 *
 * @author ilotterytea
 * @since 1.0
 */
public class CommandLoader extends ClassLoader {
    private final Map<String, Command> COMMANDS;
    private final Logger LOGGER = LoggerFactory.getLogger(CommandLoader.class);

    public CommandLoader() {
        super();
        COMMANDS = new HashMap<>();
        init();
    }

    private void init() {
        Reflections reflections = new Reflections("kz.ilotterytea.bot.builtin");

        Set<Class<? extends Command>> classes = reflections.getSubTypesOf(Command.class);

        for (Class<? extends Command> clazz : classes) {
            try {
                register(clazz.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Register the command.
     *
     * @param command Command.
     * @author ilotterytea
     * @since 1.0
     */
    public void register(Command command) {
        COMMANDS.put(command.getNameId(), command);
        LOGGER.debug(String.format("Successfully loaded the %s command!", command.getNameId()));
    }

    /**
     * Call the command.
     *
     * @return response
     * @author ilotterytea
     * @since 1.0
     */
    public Optional<Response> call(Request request) throws Exception {
        Optional<Response> response = Optional.empty();

        if (COMMANDS.containsKey(request.getMessage().getCommandId())) {
            Command cmd = COMMANDS.get(request.getMessage().getCommandId());

            List<Action> actions = request.getSession().createQuery("from Action WHERE channel = :channel AND user = :user AND commandId = :commandId ORDER BY creationTimestamp DESC", Action.class)
                    .setParameter("channel", request.getChannel())
                    .setParameter("user", request.getUser())
                    .setParameter("commandId", cmd.getNameId())
                    .getResultList();

            boolean isExecutedRecently = false;
            if (!actions.isEmpty()) {
                long currentTimestamp = new Date().getTime();
                Action action = actions.get(0);

                if (currentTimestamp - action.getCreationTimestamp().getTime() < cmd.getDelay()) {
                    isExecutedRecently = true;
                }
            }

            if (request.getUserPermission().getLevel().getValue() < cmd.getPermissions().getValue() || isExecutedRecently) {
                return Optional.empty();
            }

            Action action = new Action(request.getUser(), request.getChannel(), cmd.getNameId(), request.getEvent().getMessage());
            request.getChannel().addAction(action);
            request.getUser().addAction(action);

            request.getSession().persist(action);
            request.getSession().merge(request.getChannel());
            request.getSession().merge(request.getUser());

            response = Optional.of(cmd.run(request));
        }

        return response;
    }

    public Optional<Command> getCommand(String id, String prefix) {
        String noPrefixId = id.substring(prefix.length());
        String finalId = id.toLowerCase();
        return this.COMMANDS.values().stream().filter(c -> {
            boolean result = false;

            for (String alias : c.getAliases()) {
                String aliasCopy = alias;
                String aliasPrefix;
                if (alias.startsWith("%{np}")) {
                    aliasCopy = alias.substring(5);
                    aliasPrefix = "";
                } else {
                    aliasPrefix = prefix;
                }
                result = (aliasPrefix + aliasCopy).equals(finalId);
                if (result) break;
            }

            if (!result) {
                result = c.getNameId().equals(noPrefixId);
            }


            return result;
        }).findFirst();
    }

    /**
     * Get the loaded commands.
     *
     * @return a map of the commands.
     * @author ilotterytea
     * @since 1.0
     */
    public Map<String, Command> getCommands() {
        return COMMANDS;
    }
}
