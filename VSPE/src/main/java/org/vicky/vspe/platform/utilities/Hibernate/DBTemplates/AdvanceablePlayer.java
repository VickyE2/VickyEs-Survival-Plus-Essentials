package org.vicky.vspe.platform.utilities.Hibernate.DBTemplates;

import jakarta.persistence.*;
import org.vicky.utilities.DatabaseManager.templates.ExtendedPlayerBase;
import org.vicky.utilities.DatabaseTemplate;

import java.util.ArrayList;
import java.util.List;

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
    @JoinTable(
            name = "visited_dimensions",
            joinColumns = @JoinColumn(name = "player_id"),
            inverseJoinColumns = @JoinColumn(name = "dimension_id")
    )
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private final List<Dimension> visitedDimensions = new ArrayList<>();

    public List<Advancement> getAccomplishedAdvancements() {
        return advancements;
    }

    public void addAdvancement(Advancement advancement) {
        advancements.add(advancement);
    }

    public List<Dimension> getVisitedDimensions() {
        return visitedDimensions;
    }

    public void addVisitedDimension(Dimension dimension) {
        visitedDimensions.add(dimension);
    }
}
