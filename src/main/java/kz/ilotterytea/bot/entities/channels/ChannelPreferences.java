package kz.ilotterytea.bot.entities.channels;

import jakarta.persistence.*;
import kz.ilotterytea.bot.SharedConstants;

import java.util.HashSet;
import java.util.Set;

/**
 * Channel preferences.
 *
 * @author ilotterytea
 * @version 1.4
 */
@Entity
@Table(name = "channel_preferences")
public class ChannelPreferences {
    @Id
    @OneToOne
    @JoinColumn(name = "channel_id", updatable = false, nullable = false)
    private Channel channel;

    @Column(nullable = false)
    private String prefix;

    @Column(nullable = false)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Set<ChannelFeature> features;

    public ChannelPreferences(Channel channel) {
        this.channel = channel;
        this.prefix = SharedConstants.TWITCH_DEFAULT_PREFIX;
        this.language = SharedConstants.TWITCH_DEFAULT_LOCALE_ID;
        this.features = new HashSet<>();
    }

    public ChannelPreferences() {
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Set<ChannelFeature> getFeatures() {
        return features;
    }

    public void setFeatures(Set<ChannelFeature> features) {
        this.features = features;
    }

    public void addFeature(ChannelFeature feature) {
        this.features.add(feature);
    }

    public void removeFeature(ChannelFeature feature) {
        this.features.remove(feature);
    }
}
