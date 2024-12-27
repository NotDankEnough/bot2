package kz.ilotterytea.bot;

import io.micronaut.runtime.Micronaut;

/**
 * @author ilotterytea
 * @since 1.0
 */
public class Main {
    public static void main(String[] args) {
        Huinyabot bot = new Huinyabot();

        if (Boolean.parseBoolean(SharedConstants.PROPERTIES.getProperty("bot.enabled", "true"))) {
            Runtime.getRuntime().addShutdownHook(new Thread(bot::dispose));
            bot.init();
        }

        if (Boolean.parseBoolean(SharedConstants.PROPERTIES.getProperty("web.enabled", "true"))) {
            Micronaut.build(args)
                    .eagerInitSingletons(true)
                    .mainClass(Main.class)
                    .start();
        }
    }
}