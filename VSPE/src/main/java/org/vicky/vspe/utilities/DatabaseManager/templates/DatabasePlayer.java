package org.vicky.vspe.utilities.DatabaseManager.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(
   name = "DatabasePlayer"
)
public class DatabasePlayer {
   @Id
   private UUID id;
   @Column
   private boolean isFirstTime;
   @ManyToMany(
      cascade = {CascadeType.PERSIST},
      fetch = FetchType.EAGER
   )
   @JoinTable(
      name = "player_advancements",
      joinColumns = {@JoinColumn(
         name = "player_id"
      )},
      inverseJoinColumns = {@JoinColumn(
         name = "advancement_id"
      )}
   )
   private List<Advancement> accomplishedAdvancements = new ArrayList<>();

   public UUID getId() {
      return this.id;
   }

   public void setId(UUID id) {
      this.id = id;
   }

   public boolean isFirstTime() {
      return this.isFirstTime;
   }

   public void setFirstTime(boolean firstTime) {
      this.isFirstTime = firstTime;
   }

   public List<Advancement> getAccomplishedAdvancements() {
      return this.accomplishedAdvancements;
   }

   public void setAccomplishedAdvancements(List<Advancement> accomplishedAdvancements) {
      this.accomplishedAdvancements = accomplishedAdvancements;
   }
}
