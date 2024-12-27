package kz.ilotterytea.bot;

import io.micronaut.runtime.Micronaut;

/**
 * @author ilotterytea
 * @since 1.0
 */
public class Main {
    public static void main(String[] args) {
        Huinyabot bot = new Huinyabot();

        Runtime.getRuntime().addShutdownHook(new Thread(bot::dispose));
        bot.init();

        Micronaut.build(args)
                .eagerInitSingletons(true)
                .mainClass(Main.class)
                .start();
    }
}