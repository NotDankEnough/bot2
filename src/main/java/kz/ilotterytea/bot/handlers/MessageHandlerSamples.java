package kz.ilotterytea.bot.handlers;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import kz.ilotterytea.bot.Huinyabot;
import kz.ilotterytea.bot.api.commands.CommandException;
import kz.ilotterytea.bot.api.commands.Request;
import kz.ilotterytea.bot.api.commands.Response;
import kz.ilotterytea.bot.entities.CustomCommand;
import kz.ilotterytea.bot.entities.channels.Channel;
import kz.ilotterytea.bot.entities.channels.ChannelFeature;
import kz.ilotterytea.bot.entities.channels.ChannelPreferences;
import kz.ilotterytea.bot.entities.permissions.Permission;
import kz.ilotterytea.bot.entities.permissions.UserPermission;
import kz.ilotterytea.bot.entities.users.UserPreferences;
import kz.ilotterytea.bot.i18n.LineIds;
import kz.ilotterytea.bot.utils.HibernateUtil;
import kz.ilotterytea.bot.utils.ParsedMessage;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The samples for Twitch4j events
 *
 * @author ilotterytea
 * @since 1.0
 */
public class MessageHandlerSamples {
    private static final Logger LOG = LoggerFactory.getLogger(MessageHandlerSamples.class.getName());
    private static final Huinyabot bot = Huinyabot.getInstance();

    /**
     * Message handler sample for IRC message events.
     *
     * @author ilotterytea
     * @since 1.0
     */
    public static void channelMessageEvent(ChannelMessageEvent e) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.getTransaction().begin();

        // Getting the channel info:
        List<Channel> channels = session.createQuery("from Channel where aliasId = :aliasId AND optOutTimestamp is null", Channel.class)
                .setParameter("aliasId", e.getChannel().getId())
                .getResultList();

        Channel channel;

        if (channels.isEmpty()) {
            LOG.warn("No channel for alias ID " + e.getChannel().getId() + "! Creating a new one...");

            channel = new Channel(Integer.parseInt(e.getChannel().getId()), e.getChannel().getName());
            ChannelPreferences preferences = new ChannelPreferences(channel);
            channel.setPreferences(preferences);

            session.persist(channel);
            session.persist(preferences);
        } else {
            channel = channels.get(0);
        }

        // Getting the user info:
        List<kz.ilotterytea.bot.entities.users.User> users = session.createQuery("from User where aliasId = :aliasId AND optOutTimestamp is null", kz.ilotterytea.bot.entities.users.User.class)
                .setParameter("aliasId", e.getUser().getId())
                .getResultList();

        kz.ilotterytea.bot.entities.users.User user;

        if (users.isEmpty()) {
            LOG.warn("No user for alias ID " + e.getUser().getId() + "! Creating a new one...");

            user = new kz.ilotterytea.bot.entities.users.User(Integer.parseInt(e.getUser().getId()), e.getUser().getName());
            UserPreferences preferences = new UserPreferences(user);
            user.setPreferences(preferences);

            UserPermission userPermission = new UserPermission();
            userPermission.setLevel(Permission.USER);
            channel.addPermission(userPermission);
            user.addPermission(userPermission);

            session.persist(user);
            session.persist(preferences);
        } else {
            user = users.get(0);
        }

        // Update user's permissions:
        UserPermission userPermission = user.getPermissions()
                .stream()
                .filter(p -> p.getChannel().getAliasId().equals(channel.getAliasId()))
                .findFirst()
                .orElseGet(() -> {
                    UserPermission permission1 = new UserPermission();
                    permission1.setLevel(Permission.USER);
                    channel.addPermission(permission1);
                    user.addPermission(permission1);

                    return permission1;
                });

        if (userPermission.getLevel().getValue() == Permission.SUSPENDED.getValue()) {
            session.close();
            return;
        }

        if (Objects.equals(e.getChannel().getId(), e.getUser().getId())) {
            userPermission.setLevel(Permission.BROADCASTER);
        } else if (e.getMessageEvent().getBadges().containsKey("moderator")) {
            userPermission.setLevel(Permission.MOD);
        } else if (e.getMessageEvent().getBadges().containsKey("vip")) {
            userPermission.setLevel(Permission.VIP);
        } else {
            userPermission.setLevel(Permission.USER);
        }

        session.persist(userPermission);
        session.getTransaction().commit();

        if (channel.getPreferences().getFeatures().contains(ChannelFeature.SILENT_MODE)) {
            session.close();
            return;
        }

        String msg = e.getMessage();

        final Optional<ParsedMessage> parsedMessage = ParsedMessage.parse(msg, channel.getPreferences().getPrefix());

        // Processing the command:
        if (parsedMessage.isPresent()) {
            session.getTransaction().begin();
            Request request = new Request(
                    session,
                    e,
                    parsedMessage.get(),
                    channel,
                    user,
                    userPermission
            );

            try {
                Optional<Response> responseOptional = bot.getLoader().call(request);

                if (responseOptional.isPresent()) {
                    Response response = responseOptional.get();

                    if (response.isMultiple()) {
                        for (String message : response.getMultiple()) {
                            bot.getClient().getChat().sendMessage(channel.getAliasName(), message);
                        }
                    } else if (response.isSingle()) {
                        bot.getClient().getChat().sendMessage(channel.getAliasName(), response.getSingle());
                    }
                }
            } catch (CommandException exception) {
                bot.getClient().getChat().sendMessage(channel.getAliasName(), bot.getLocale()
                        .formattedText(request.getChannel().getPreferences().getLanguage(),
                                LineIds.ERROR_TEMPLATE,
                                request.getUser().getAliasName(),
                                exception.getMessage()
                        )
                );
                LOG.info("An error occurred while executing the command {}: {}", request.getMessage().getCommandId(), exception.getMessage());
            } catch (Exception exception) {
                bot.getClient().getChat().sendMessage(channel.getAliasName(), bot.getLocale()
                        .formattedText(request.getChannel().getPreferences().getLanguage(),
                                LineIds.ERROR_TEMPLATE,
                                request.getUser().getAliasName(),
                                CommandException.somethingWentWrong(request).getMessage()
                        )
                );
                LOG.error("An error occurred while executing the command {}", request.getMessage().getCommandId(), exception);
            }

            if (session.getTransaction().isActive()) session.getTransaction().commit();
            session.close();
            return;
        }

        // Processing the custom commands:
        List<CustomCommand> commands = session.createQuery("from CustomCommand where channel = :channel AND name = :name AND isEnabled = true", CustomCommand.class)
                .setParameter("channel", channel)
                .setParameter("name", msg)
                .getResultList();

        if (!commands.isEmpty()) {
            for (CustomCommand command : commands) {
                bot.getClient().getChat().sendMessage(
                        channel.getAliasName(),
                        command.getMessage()
                );
            }
        }

        session.close();
    }
}
