package org.vicky.vspe.utilities.DatabaseManager.templates;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "AdvancementManager"
)
public class AdvancementManager {
    @Id
    private String id;
    @OneToMany(
            cascade = {CascadeType.ALL},
            fetch = FetchType.LAZY
    )
    @JoinColumn(
            name = "manager_id"
    )
    private List<Advancement> advancements = new ArrayList<>();

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
}
