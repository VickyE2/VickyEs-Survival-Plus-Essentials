package org.vicky.vspe.utilities.DBTemplates;

import jakarta.persistence.*;
import org.vicky.utilities.DatabaseTemplate;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "AdvancementManager")
public class AdvancementManager implements DatabaseTemplate {
    @Id
    private String id;
    @OneToMany(
            cascade = {CascadeType.ALL},
            fetch = FetchType.EAGER
    )
    @JoinColumn(
            name = "manager_id"
    )
    private List<Advancement> advancements = new ArrayList<>();

    public AdvancementManager() {}

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Advancement> getAdvancements() {
        return this.advancements;
    }


    public void setAdvancements(List<Advancement> advancements) {
        this.advancements = advancements;
    }

    public void addAdvancement(Advancement advancement) {
        this.advancements.add(advancement);
    }
}
