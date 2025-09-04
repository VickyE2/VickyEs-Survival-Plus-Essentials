package org.vicky.vspe.platform.utilities.Hibernate.DBTemplates;

import org.vicky.shaded.jakarta.persistence.Column;
import org.vicky.shaded.jakarta.persistence.Entity;
import org.vicky.shaded.jakarta.persistence.Id;
import org.vicky.shaded.jakarta.persistence.Table;
import org.vicky.utilities.DatabaseTemplate;

import java.util.UUID;

@Entity
@Table(
        name = "Dimension"
)
public class Dimension implements DatabaseTemplate {
    @Id
    @Column(name = "dimension_id", unique = true, nullable = false)
    private String id;
    @Column(
            name = "Name",
            nullable = false
    )
    private String name;
    @Column(
            name = "Status"
    )
    private boolean isLoaded;

    public UUID getId() {
        return UUID.fromString(id);
    }

    public void setId(UUID id) {
        this.id = id.toString();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getState() {
        return isLoaded;
    }

    public void setState(boolean loaded) {
        isLoaded = loaded;
    }
}
