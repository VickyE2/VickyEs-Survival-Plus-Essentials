package org.vicky.vspe.utilities.DBTemplates;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.vicky.utilities.DatabaseTemplate;

import java.util.UUID;

@Entity
@Table(
        name = "Dimension"
)
public class Dimension implements DatabaseTemplate {
    @Id
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

    public Dimension() {}

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
