package kz.ilotterytea.bot.web.controllers;

import com.github.twitch4j.helix.domain.User;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.views.View;
import kz.ilotterytea.bot.Huinyabot;
import kz.ilotterytea.bot.SharedConstants;
import kz.ilotterytea.bot.entities.channels.Channel;
import kz.ilotterytea.bot.utils.HibernateUtil;
import kz.ilotterytea.bot.web.singletons.DocsSingleton;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Controller()
public class ViewsController {
    private record ChannelCatalogueRecord(String id, String username, String image_url, Boolean opted_out) {
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
