package org.vicky.vspe.systems.Dimension.Generator.utils.Structures;

import org.vicky.vspe.systems.Dimension.Generator.utils.Utilities;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;

public class TextStructure implements Ymlable {
   private final StringBuilder structure;
   private final String id;

   public TextStructure(String id, String builder) {
      this.id = Utilities.getCleanedID(id);
      this.structure = new StringBuilder(builder);
   }

   public String getId() {
      return this.id;
   }

   @Override
   public StringBuilder getYml() {
      return this.structure;
   }
}
