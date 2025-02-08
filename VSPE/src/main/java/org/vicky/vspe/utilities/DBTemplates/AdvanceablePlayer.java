package org.vicky.vspe.utilities.DBTemplates;

import jakarta.persistence.*;
import org.vicky.utilities.DatabaseManager.templates.DatabasePlayer;
import org.vicky.utilities.DatabaseTemplate;

import java.util.ArrayList;
import java.util.List;

@Entity
public class AdvanceablePlayer extends DatabasePlayer implements DatabaseTemplate {
    @JoinTable(
            name = "advancements",
            joinColumns = @JoinColumn(name = "player_id"),
            inverseJoinColumns = @JoinColumn(name = "advancement_id")
    )
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Advancement> advancements = new ArrayList<>();

    public List<Advancement> getAccomplishedAdvancements() {
        return advancements;
    }

    public void addAdvancement(Advancement advancement){
        advancements.add(advancement);
    }

    public AdvanceablePlayer() {}
}
