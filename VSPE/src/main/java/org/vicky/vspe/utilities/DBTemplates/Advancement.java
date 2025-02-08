package org.vicky.vspe.utilities.DBTemplates;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.vicky.utilities.DatabaseTemplate;

import java.util.UUID;

@Entity
@Table(
        name = "RegisteredAdvancement"
)
public class Advancement implements DatabaseTemplate {
    @Id
    private String id;
    @Column(
            name = "Name",
            nullable = false
    )
    private String name;

    public Advancement() {}

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
}
