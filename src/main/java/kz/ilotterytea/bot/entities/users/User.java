package kz.ilotterytea.bot.entities.users;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

/**
 * @author ilotterytea
 * @version 1.0
 */
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, unique = true, nullable = false)
    private Integer id;

    @Column(name = "alias_id", unique = true, updatable = false, nullable = false)
    private Integer aliasId;

    @Column(name = "alias_name", nullable = false)
    private String aliasName;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", updatable = false, nullable = false)
    private Date creationTimestamp;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    private Date updateTimestamp;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "opt_outed_at")
    private Date optOutTimestamp;

    @OneToOne(mappedBy = "user", cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    private UserPreferences preferences;

    public User(Integer aliasId, String aliasName) {
        this.aliasId = aliasId;
        this.aliasName = aliasName;
    }

    public User() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAliasId() {
        return aliasId;
    }

    public void setAliasId(Integer aliasId) {
        this.aliasId = aliasId;
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }

    public Date getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(Date creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public Date getUpdateTimestamp() {
        return updateTimestamp;
    }

    public void setUpdateTimestamp(Date updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }

    public Date getOptOutTimestamp() {
        return optOutTimestamp;
    }

    public void setOptOutTimestamp(Date optOutTimestamp) {
        this.optOutTimestamp = optOutTimestamp;
    }

    public UserPreferences getPreferences() {
        return preferences;
    }

    public void setPreferences(UserPreferences preferences) {
        preferences.setUser(this);
        this.preferences = preferences;
    }
}
