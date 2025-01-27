package org.vicky.vspe.utilities.DatabaseManager.templates;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "DatabasePlayer"
)
public class DatabasePlayer {
    @Id
    private UUID id;
    @Column
    private boolean isFirstTime;
    @ManyToMany(
            cascade = {CascadeType.PERSIST},
            fetch = FetchType.EAGER
    )
    @JoinTable(
            name = "player_advancements",
            joinColumns = {@JoinColumn(
                    name = "player_id"
            )},
            inverseJoinColumns = {@JoinColumn(
                    name = "advancement_id"
            )}
    )
    private List<Advancement> accomplishedAdvancements = new ArrayList<>();

    public UUID getId() {
        return this.id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public boolean isFirstTime() {
        return this.isFirstTime;
    }

    public void setFirstTime(boolean firstTime) {
        this.isFirstTime = firstTime;
    }

    public List<Advancement> getAccomplishedAdvancements() {
        return this.accomplishedAdvancements;
    }

    public void setAccomplishedAdvancements(List<Advancement> accomplishedAdvancements) {
        this.accomplishedAdvancements = accomplishedAdvancements;
    }
}
