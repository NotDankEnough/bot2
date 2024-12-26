package kz.ilotterytea.bot.api.commands;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import kz.ilotterytea.bot.entities.channels.Channel;
import kz.ilotterytea.bot.entities.permissions.UserPermission;
import kz.ilotterytea.bot.entities.users.User;
import kz.ilotterytea.bot.utils.ParsedMessage;
import org.hibernate.Session;

public class Request {
    private final Session session;
    private final ChannelMessageEvent event;
    private final ParsedMessage message;

    private final Channel channel;
    private final User user;
    private final UserPermission userPermission;

    public Request(
            Session session,
            ChannelMessageEvent event,
            ParsedMessage message,
            Channel channel,
            User user,
            UserPermission userPermission
    ) {
        this.session = session;
        this.event = event;
        this.message = message;
        this.channel = channel;
        this.user = user;
        this.userPermission = userPermission;
    }

    public Session getSession() {
        return session;
    }

    public ChannelMessageEvent getEvent() {
        return event;
    }

    public ParsedMessage getMessage() {
        return message;
    }

    public Channel getChannel() {
        return channel;
    }

    public User getUser() {
        return user;
    }

    public UserPermission getUserPermission() {
        return userPermission;
    }
}
