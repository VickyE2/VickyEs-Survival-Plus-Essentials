package org.vicky.vspe.utilities.DatabaseManager.templates;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

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
