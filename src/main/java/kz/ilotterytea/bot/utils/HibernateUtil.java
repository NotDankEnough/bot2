package kz.ilotterytea.bot.utils;

import kz.ilotterytea.bot.entities.Action;
import kz.ilotterytea.bot.entities.CustomCommand;
import kz.ilotterytea.bot.entities.Timer;
import kz.ilotterytea.bot.entities.channels.Channel;
import kz.ilotterytea.bot.entities.channels.ChannelPreferences;
import kz.ilotterytea.bot.entities.events.Event;
import kz.ilotterytea.bot.entities.events.subscriptions.EventSubscription;
import kz.ilotterytea.bot.entities.permissions.UserPermission;
import kz.ilotterytea.bot.entities.users.User;
import kz.ilotterytea.bot.entities.users.UserPreferences;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.Properties;

/**
 * @author ilotterytea
 * @version 1.0
 */
public class HibernateUtil {
    private static SessionFactory sessionFactory;

    public static void initSessionFactory(Properties properties) {
        assert sessionFactory == null;

        Configuration config = new Configuration();

        // Adding entity classes
        config.addAnnotatedClass(Channel.class)
                .addAnnotatedClass(ChannelPreferences.class)
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(UserPreferences.class)
                .addAnnotatedClass(Event.class)
                .addAnnotatedClass(EventSubscription.class)
                .addAnnotatedClass(CustomCommand.class)
                .addAnnotatedClass(UserPermission.class)
                .addAnnotatedClass(Timer.class)
                .addAnnotatedClass(Action.class);

        // Configuring the connection
        String jdbcUrl = properties.getProperty("hibernate.connection.url", null);
        String jdbcUsername = properties.getProperty("hibernate.connection.username", null);
        String jdbcPassword = properties.getProperty("hibernate.connection.password", null);

        if (jdbcUrl == null || jdbcUsername == null || jdbcPassword == null) {
            throw new RuntimeException("connection.url, connection.username and connection.password must be set in config.properties for database connections!");
        }

        config.setProperty("hibernate.connection.driver_class", properties.getProperty("connection.driver_class", "org.postgresql.Driver"));
        config.setProperty("hibernate.connection.url", jdbcUrl);
        config.setProperty("hibernate.connection.username", jdbcUsername);
        config.setProperty("hibernate.connection.password", jdbcPassword);

        config.setProperty("hibernate.dialect", properties.getProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect"));
        config.setProperty("current_session_context_class", "thread");
        config.setProperty("cache.provider_class", "org.hibernate.cache.internal.NoCacheProvider");
        config.setProperty("hibernate.show_sql", "false");
        config.setProperty("hibernate.hbm2ddl.auto", "update");

        sessionFactory = config.buildSessionFactory();
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
