package org.vicky.vspe.systems.Dimension.Generator.utils;

public enum ArithmeticOperation {
   ADD("+"),
   SUBTRACT("-"),
   DIVIDE("/"),
   MULTIPLY("*");

   private final String operator;

   private ArithmeticOperation(String operator) {
      this.operator = operator;
   }

   @Override
   public String toString() {
      return this.operator;
   }
}
