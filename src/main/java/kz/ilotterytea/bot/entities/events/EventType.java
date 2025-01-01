package kz.ilotterytea.bot.entities.events;

import java.util.Optional;

/**
 * Types for event.
 *
 * @author ilotterytea
 * @version 1.6
 */
public enum EventType {
    /**
     * Custom event type.
     */
    CUSTOM("custom", 99),

    /**
     * "Stream live" event type.
     */
    LIVE("live", 0),

    /**
     * "Stream offline" event type.
     */
    OFFLINE("offline", 1),
    MESSAGE("message", 10),
    GITHUB("github", 20);

    private final String name;
    private final Integer id;

    EventType(String name, Integer id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Integer getId() {
        return id;
    }

    public static Optional<EventType> findEventTypeById(String id) {
        for (EventType eventType : EventType.values()) {
            if (eventType.name.equals(id)) {
                return Optional.of(eventType);
            }
        }

        return Optional.empty();
    }
}
