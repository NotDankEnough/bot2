package kz.ilotterytea.bot.web.controllers;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.views.View;
import kz.ilotterytea.bot.SharedConstants;

@Controller("/")
public class PageController {
    private String appName;

    public PageController(@Property(name = "micronaut.application.name") String appName) {
        this.appName = appName;
    }

    @Get
    @View("index")
    public HttpResponse<?> getIndex() {
        return HttpResponse.ok(CollectionUtils.mapOf(
                "app_name", appName,
                "contact_url", SharedConstants.PROPERTIES.getProperty("web.contact.url", "#"),
                "contact_name", SharedConstants.PROPERTIES.getProperty("web.contact.name", "a frog")
        ));
    }
}
