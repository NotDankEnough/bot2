package kz.ilotterytea.bot.web.controllers;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.views.View;
import kz.ilotterytea.bot.SharedConstants;
import kz.ilotterytea.bot.web.singletons.DocsSingleton;

import java.util.Objects;

@Controller("/")
public class PageController {
    private String appName;

    private DocsSingleton docs;

    public PageController(@Property(name = "micronaut.application.name") String appName, DocsSingleton docs) {
        this.appName = appName;
        this.docs = docs;
    }

    @Get
    @View("index")
    public HttpResponse<?> getIndex() {
        return HttpResponse.ok(CollectionUtils.mapOf(
                "app_name", appName,
                "contact_url", SharedConstants.PROPERTIES.getProperty("web.contact.url", "#"),
                "contact_name", SharedConstants.PROPERTIES.getProperty("web.contact.name", "a frog"),
                "is_wiki", false
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
                "is_wiki", true
        ));
    }
}
