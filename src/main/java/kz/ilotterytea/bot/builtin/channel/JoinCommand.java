package kz.ilotterytea.bot.builtin.channel;

import kz.ilotterytea.bot.Huinyabot;
import kz.ilotterytea.bot.api.commands.Command;
import kz.ilotterytea.bot.api.commands.Request;
import kz.ilotterytea.bot.api.commands.Response;
import kz.ilotterytea.bot.entities.channels.Channel;
import kz.ilotterytea.bot.entities.channels.ChannelPreferences;
import kz.ilotterytea.bot.entities.users.User;
import kz.ilotterytea.bot.i18n.LineIds;
import org.hibernate.Session;

import java.util.Collections;
import java.util.List;

/**
 * Join command.
 *
 * @author ilotterytea
 * @since 1.1
 */
public class JoinCommand implements Command {
    @Override
    public String getNameId() {
        return "join";
    }

    @Override
    public int getDelay() {
        return 120000;
    }

    @Override
    public List<String> getOptions() {
        return List.of("silent", "тихо", "only-listen");
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("зайти");
    }

    @Override
    public Response run(Request request) {
        Channel channel = request.getChannel();
        User user = request.getUser();
        Session session = request.getSession();

        // Getting the sender's local channel info if it exists:
        List<Channel> userChannels = session.createQuery("from Channel where aliasId = :aliasId", Channel.class)
                .setParameter("aliasId", user.getAliasId())
                .getResultList();

        Channel userChannel;

        // Creating a new channel if it does not exist:
        if (userChannels.isEmpty()) {
            userChannel = new Channel(user.getAliasId(), user.getAliasName());
            ChannelPreferences preferences = new ChannelPreferences(userChannel);
            userChannel.setPreferences(preferences);

            session.persist(userChannel);
            session.persist(preferences);
        } else {
            userChannel = userChannels.get(0);

            // If the channel has already been opt-outed, opt-in:
            if (userChannel.getOptOutTimestamp() != null) {
                userChannel.setOptOutTimestamp(null);
                userChannel.setAliasName(user.getAliasName());

                session.persist(userChannel);
            } else {
                return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                        channel.getPreferences().getLanguage(),
                        LineIds.C_JOIN_ALREADYIN,
                        channel.getAliasName()
                ));
            }
        }

        Huinyabot.getInstance().getClient().getChat().joinChannel(userChannel.getAliasName());

        return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                channel.getPreferences().getLanguage(),
                LineIds.C_JOIN_SUCCESS,
                userChannel.getAliasName()
        ));
    }
}
