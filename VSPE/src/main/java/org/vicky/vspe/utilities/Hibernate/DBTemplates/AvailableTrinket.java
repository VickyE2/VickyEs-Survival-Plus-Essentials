package org.vicky.vspe.utilities.Hibernate.DBTemplates;

import jakarta.persistence.*;
import org.bukkit.inventory.ItemStack;
import org.hibernate.annotations.NaturalId;
import org.vicky.guiparent.GuiCreator;
import org.vicky.utilities.DatabaseTemplate;
import org.vicky.vspe.features.CharmsAndTrinkets.BaseTrinket;
import org.vicky.vspe.features.CharmsAndTrinkets.exceptions.NullManagerTrinket;

import java.util.Optional;
import java.util.UUID;

import static org.vicky.vspe.utilities.global.GlobalResources.trinketManager;

@Entity
@Table(name = "Available Trinkets")
public class AvailableTrinket implements DatabaseTemplate {
    @Id
    @Column(name = "trinket_id", unique = true, nullable = false)
    private String id;

    @NaturalId
    @Column(
            name = "Name",
            nullable = false
    )
    private String name;

    public AvailableTrinket() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Transient
    public UUID getIdAsUUID() {
        return UUID.fromString(id);
    }

    @Transient
    public ItemStack getItem() throws NullManagerTrinket {
        Optional<BaseTrinket> trinket = trinketManager.getTrinketById(this.id);
        if (trinket.isEmpty()) {
            throw new NullManagerTrinket("Trinket could be found in database but could not be found on manager map.");
        }
        return trinket.get().getIcon();
    }

    @Transient
    public GuiCreator.ItemConfig getRawItem() throws NullManagerTrinket {
        Optional<BaseTrinket> trinket = trinketManager.getTrinketById(this.id);
        if (trinket.isEmpty()) {
            throw new NullManagerTrinket("Trinket could be found in database but could not be found on manager map.");
        }
        return trinket.get().getRawIcon();
    }
}
