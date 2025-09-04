package org.vicky.vspe.platform.utilities.Hibernate.DBTemplates;

import org.hibernate.annotations.NaturalId;
import org.vicky.shaded.jakarta.persistence.*;
import org.vicky.utilities.DatabaseTemplate;

import java.util.UUID;

@Access(AccessType.FIELD)
@Entity
@Table(name = "Registered_Advancements")
public class Advancement implements DatabaseTemplate {
    @Id
    @Column(name = "advancement_id", unique = true, nullable = false, updatable = false)
    public UUID id = UUID.randomUUID();

    @NaturalId
    @Column(
            name = "Name",
            nullable = false
    )
    private String name;

    public UUID getId() {
        return id;
    }

    @Transient
    public String getIdAsString() {
        if (id != null)
            return id.toString();
        return "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return getId().toString();
    }
}
