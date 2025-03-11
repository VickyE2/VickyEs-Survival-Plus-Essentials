package org.vicky.vspe.utilities.Hibernate.DBTemplates;

import jakarta.persistence.*;
import org.vicky.utilities.DatabaseManager.templates.DatabasePlayer;
import org.vicky.utilities.DatabaseManager.templates.ExtendedPlayerBase;
import org.vicky.utilities.DatabaseTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "AdvanceablePlayers")
public class AdvanceablePlayer extends ExtendedPlayerBase implements DatabaseTemplate {
    @JoinTable(
            name = "advancements",
            joinColumns = @JoinColumn(name = "player_id"),
            inverseJoinColumns = @JoinColumn(name = "advancement_id")
    )
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private final List<Advancement> advancements = new ArrayList<>();

    public List<Advancement> getAccomplishedAdvancements() {
        return advancements;
    }

    public void addAdvancement(Advancement advancement) {
        advancements.add(advancement);
    }
}
