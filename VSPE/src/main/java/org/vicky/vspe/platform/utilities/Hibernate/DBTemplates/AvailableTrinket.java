package org.vicky.vspe.platform.utilities.Hibernate.DBTemplates;

import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;
import org.vicky.utilities.DatabaseTemplate;
import org.vicky.vspe.platform.PlatformItem;
import org.vicky.vspe.platform.VSPEPlatformPlugin;
import org.vicky.vspe.platform.features.CharmsAndTrinkets.PlatformTrinket;
import org.vicky.vspe.platform.features.CharmsAndTrinkets.exceptions.NullManagerTrinket;

import java.util.Optional;
import java.util.UUID;

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
    public PlatformItem getItem() throws NullManagerTrinket {
        Optional<PlatformTrinket> trinket = VSPEPlatformPlugin.trinketManager().getTrinketById(this.id);
        if (trinket.isEmpty()) {
            throw new NullManagerTrinket("Trinket could be found in database but could not be found on manager map.");
        }
        return trinket.get().getIcon();
    }

    @Transient
    public PlatformItem getRawItem() throws NullManagerTrinket {
        Optional<PlatformTrinket> trinket = VSPEPlatformPlugin.trinketManager().getTrinketById(this.id);
        if (trinket.isEmpty()) {
            throw new NullManagerTrinket("Trinket could be found in database but could not be found on manager map.");
        }
        return trinket.get().getIcon();
    }
}
