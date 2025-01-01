package kz.ilotterytea.bot.web.controllers;

import com.github.twitch4j.helix.domain.User;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.views.View;
import kz.ilotterytea.bot.Huinyabot;
import kz.ilotterytea.bot.SharedConstants;
import kz.ilotterytea.bot.entities.channels.Channel;
import kz.ilotterytea.bot.entities.channels.ChannelFeature;
import kz.ilotterytea.bot.entities.events.EventFlag;
import kz.ilotterytea.bot.utils.HibernateUtil;
import kz.ilotterytea.bot.utils.StringUtils;
import kz.ilotterytea.bot.web.singletons.DocsSingleton;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Controller()
public class ViewsController {
    private record ChannelCatalogueRecord(String id, String username, String image_url, Boolean opted_out) {
    }

    private record ChannelEventRecord(String name, String event_type, String message, String flags,
                                      String subscribers) {
    }

    private record ChannelCommandRecord(String name, String message, String aliases, String is_global,
                                        String is_enabled) {
    }

    private record ChannelTimerRecord(String name, String message, String interval, String last_executed) {
    }

    private final Logger log = LoggerFactory.getLogger(ViewsController.class);
    private final Huinyabot bot;
    private final String appName;
    private final DocsSingleton docs;

    public ViewsController(@Property(name = "micronaut.application.name") String appName, DocsSingleton docs) {
        this.appName = appName;
        this.docs = docs;
        this.bot = Huinyabot.getInstance();
    }

    @Get
    @View("index")
    public HttpResponse<?> getIndex() {
        return HttpResponse.ok(CollectionUtils.mapOf(
                "app_name", appName,
                "contact_url", SharedConstants.PROPERTIES.getProperty("web.contact.url", "#"),
                "contact_name", SharedConstants.PROPERTIES.getProperty("web.contact.name", "a frog"),
                "title", "home"
        ));
    }

    @Get("/wiki")
    @View("wiki_page")
    public HttpResponse<?> getMainWikiPage() {
        return getWikiPage("README");
    }

    @Get("/wiki/{page}")
    @View("wiki_page")
    public HttpResponse<?> getSpecifiedWikiPage(String page) {
        return getWikiPage(page);
    }

    @Get("/catalogue")
    @View("channel_catalogue")
    public HttpResponse<?> getChannelCatalogue() {
        if (bot == null || bot.getClient() == null) {
            log.warn("Failed to send channel catalogue because bot instance is null");
            return HttpResponse.temporaryRedirect(URI.create("/"));
        }

        Session session = HibernateUtil.getSessionFactory().openSession();
        List<Channel> internalChannels = session.createQuery("from Channel", Channel.class).getResultList();
        session.close();

        ArrayList<ChannelCatalogueRecord> channels = new ArrayList<>();

        try {
            List<User> userData = bot.getClient().getHelix()
                    .getUsers(
                            bot.getCredential().getAccessToken(),
                            internalChannels.stream().map((x) -> x.getAliasId().toString()).toList(),
                            null
                    ).execute()
                    .getUsers();

            userData.forEach((x) -> {
                Optional<Channel> c = internalChannels.stream().filter((y) -> y.getAliasId().toString().equals(x.getId())).findFirst();
                if (c.isEmpty()) return;

                channels.add(new ChannelCatalogueRecord(
                        c.get().getAliasId().toString(),
                        c.get().getAliasName(),
                        x.getProfileImageUrl(),
                        c.get().getOptOutTimestamp() != null
                ));
            });
        } catch (Exception e) {
            log.warn("Failed to get users", e);
            return HttpResponse.temporaryRedirect(URI.create("/"));
        }

        return HttpResponse.ok(CollectionUtils.mapOf(
                "app_name", appName,
                "contact_url", SharedConstants.PROPERTIES.getProperty("web.contact.url", "#"),
                "contact_name", SharedConstants.PROPERTIES.getProperty("web.contact.name", "a frog"),
                "channels", channels,
                "title", "channel catalogue"
        ));
    }

    @Get("/channel/{id}")
    @View("channel")
    public HttpResponse<?> getChannel(Integer id) {
        if (bot == null || bot.getClient() == null) {
            log.warn("Failed to send a channel because bot instance is null");
            return HttpResponse.temporaryRedirect(URI.create("/"));
        }

        Session session = HibernateUtil.getSessionFactory().openSession();
        List<Channel> internalChannels = session.createQuery("from Channel where aliasId = :aliasId", Channel.class)
                .setParameter("aliasId", id)
                .getResultList();

        if (internalChannels.isEmpty()) {
            session.close();
            return HttpResponse.temporaryRedirect(URI.create("/404"));
        }

        Channel internalChannel = internalChannels.get(0);
        User user = null;

        try {
            List<User> userData = bot.getClient().getHelix()
                    .getUsers(
                            bot.getCredential().getAccessToken(),
                            List.of(internalChannel.getAliasId().toString()),
                            null
                    ).execute()
                    .getUsers();

            user = userData.get(0);
        } catch (Exception e) {
            log.error("Failed to get Twitch user", e);
        }

        if (user == null) {
            session.close();
            return HttpResponse.temporaryRedirect(URI.create("/404"));
        }

        Hibernate.initialize(internalChannel.getEvents());
        Hibernate.initialize(internalChannel.getCommands());
        Hibernate.initialize(internalChannel.getTimers());

        ArrayList<ChannelEventRecord> events = new ArrayList<>();
        internalChannel.getEvents().forEach((x) -> {
            Hibernate.initialize(x.getSubscriptions());
            events.add(new ChannelEventRecord(
                    x.getTargetAliasId() == null ? x.getCustomAliasId() : x.getTargetAliasId().toString(),
                    x.getEventType().getName(),
                    x.getMessage(),
                    String.join(", ", x.getFlags().stream().map(EventFlag::getName).toList()),
                    String.valueOf(x.getSubscriptions().size())
            ));
        });

        ArrayList<ChannelCommandRecord> commands = new ArrayList<>();
        internalChannel.getCommands().forEach((x) -> {
            commands.add(new ChannelCommandRecord(
                    x.getName(),
                    x.getMessage(),
                    String.join(", ", x.getAliases()),
                    x.getGlobal() ? "✅" : "❌",
                    x.getEnabled() ? "✅" : "❌"
            ));
        });

        ArrayList<ChannelTimerRecord> timers = new ArrayList<>();
        internalChannel.getTimers().forEach((x) -> {
            timers.add(new ChannelTimerRecord(
                    x.getName(),
                    x.getMessage(),
                    StringUtils.formatTimestamp(x.getIntervalMilliseconds() / 1000),
                    StringUtils.formatTimestamp((Timestamp.from(Instant.now()).getTime() - x.getLastTimeExecuted().getTime()) / 1000)
            ));
        });

        session.close();
        return HttpResponse.ok(CollectionUtils.mapOf(
                "app_name", appName,
                "contact_url", SharedConstants.PROPERTIES.getProperty("web.contact.url", "#"),
                "contact_name", SharedConstants.PROPERTIES.getProperty("web.contact.name", "a frog"),
                "title", internalChannel.getAliasName(),

                "username", internalChannel.getAliasName(),
                "pfp", user.getProfileImageUrl(),
                "description", user.getDescription(),
                "joined", StringUtils.formatTimestamp((Timestamp.from(Instant.now()).getTime() - internalChannel.getCreationTimestamp().getTime()) / 1000),
                "opted_out", internalChannel.getOptOutTimestamp() != null,
                "silent_mode", internalChannel.getPreferences().getFeatures().contains(ChannelFeature.SILENT_MODE),
                "events", events,
                "commands", commands,
                "timers", timers
        ));
    }

    @Get("/404")
    public HttpResponse<?> notFound() {
        return HttpResponse.notFound("Not found").contentType(MediaType.TEXT_PLAIN);
    }

    private HttpResponse<?> getWikiPage(String page) {
        String content = docs.getDoc(page);

        if (Objects.equals(page, "summary") || content == null) {
            return HttpResponse.notFound(String.format("Page %s not exists", page));
        }

        String summary = docs.getDoc("summary");

        return HttpResponse.ok(CollectionUtils.mapOf(
                "app_name", appName,
                "contact_url", SharedConstants.PROPERTIES.getProperty("web.contact.url", "#"),
                "contact_name", SharedConstants.PROPERTIES.getProperty("web.contact.name", "a frog"),
                "summary", summary,
                "content", content,
                "page_name", page,
                "is_wiki", true,
                "title", (!page.equals("README") ? page + " - " : "") + "wiki"
        ));
    }
}
