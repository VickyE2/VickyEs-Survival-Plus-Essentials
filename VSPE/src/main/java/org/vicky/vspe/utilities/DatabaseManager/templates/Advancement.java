package org.vicky.vspe.utilities.DatabaseManager.templates;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(
        name = "Advancement"
)
public class Advancement {
    @Id
    private UUID id;
    @Column(
            name = "Name",
            nullable = false
    )
    private String name;

    public UUID getId() {
        return this.id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
