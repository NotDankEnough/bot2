package kz.ilotterytea.bot.entities.events;

import jakarta.persistence.*;
import kz.ilotterytea.bot.entities.channels.Channel;
import kz.ilotterytea.bot.entities.events.subscriptions.EventSubscription;

import java.util.HashSet;
import java.util.Set;

/**
 * Entity for events.
 *
 * @author ilotterytea
 * @version 1.6
 */
@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "channel_id", updatable = false, nullable = false)
    private Channel channel;

    @Column(name = "target_alias_id", updatable = false)
    private Integer targetAliasId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false)
    private EventType eventType;

    @Column(name = "custom_alias_id", updatable = false)
    private String customAliasId;

    @Column(name = "message", nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Set<EventFlag> flags;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<EventSubscription> subscriptions;

    public Event(Integer targetAliasId, EventType eventType, String message) {
        this.targetAliasId = targetAliasId;
        this.eventType = eventType;
        this.customAliasId = null;
        this.message = message;
        this.flags = new HashSet<>();
        this.subscriptions = new HashSet<>();
    }

    public Event(String customAliasId, EventType eventType, String message) {
        this.targetAliasId = null;
        this.eventType = eventType;
        this.customAliasId = customAliasId;
        this.message = message;
        this.flags = new HashSet<>();
        this.subscriptions = new HashSet<>();
    }

    public Event() {
    }

    @PrePersist
    private void prePersist() {
        if (eventType.getId() < EventType.GITHUB.getId() && targetAliasId == null) {
            throw new IllegalStateException("targetAliasId is required for non-custom events!");
        }

        if (eventType.getId() >= EventType.GITHUB.getId() && customAliasId == null) {
            throw new IllegalStateException("customAliasId is required for custom events!");
        }
    }

    public Integer getId() {
        return id;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Integer getTargetAliasId() {
        return targetAliasId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getCustomAliasId() {
        return customAliasId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Set<EventFlag> getFlags() {
        return flags;
    }

    public void setFlags(Set<EventFlag> flags) {
        this.flags = flags;
    }

    public void addFlag(EventFlag flag) {
        this.flags.add(flag);
    }

    public void removeFlag(EventFlag flag) {
        this.flags.remove(flag);
    }

    public Set<EventSubscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(Set<EventSubscription> subscriptions) {
        for (EventSubscription subscription : subscriptions) {
            subscription.setEvent(this);
        }

        this.subscriptions = subscriptions;
    }

    public void addSubscription(EventSubscription subscription) {
        subscription.setEvent(this);
        this.subscriptions.add(subscription);
    }

    public void removeSubscription(EventSubscription subscription) {
        subscription.setEvent(null);
        this.subscriptions.remove(subscription);
    }
}
