package org.vicky.vspe.systems.Dimension.Generator.utils.Structures;

import org.vicky.vspe.utilities.Math.Vector3;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.vicky.vspe.systems.Dimension.Generator.utils.Utilities.getResourceAsFile;

public class BaseStructure {
    public String id;
    public File schemFileName;
    private StringBuilder structureScript;
    private Map<String, Integer> numberVariables;
    private Map<String, String> stringVariables;
    private Map<String, Boolean> booleanVariables;
    private Map<String, Object> variables;
    private Map<String, Object> mappedVariables;

    public BaseStructure(String id) {
        structureScript = new StringBuilder();
        this.id = id.toLowerCase().replaceAll("[^a-z]", "_");
        numberVariables = new HashMap<>();
        stringVariables = new HashMap<>();
        booleanVariables = new HashMap<>();
        variables = new HashMap<>();
    }

    public BaseStructure(File schemFilename) {
        this.schemFileName = schemFilename;
    }

    public static File getSchemFile(String schemFileName) {
        try {
            return getResourceAsFile("assets/structures/" + schemFileName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addBooleanVariable(String variableName, boolean variableValue) {
        booleanVariables.put(variableName, variableValue);
    }

    public void addStringVariable(String variableName, String variableValue) {
        stringVariables.put(variableName, variableValue);
    }

    public void addIntegerVariable(String variableName, int variableValue) {
        numberVariables.put(variableName, variableValue);
    }

    public void addMappedVariable(String variableName, String variable) {
        mappedVariables.put(variableName, variable);
    }

    public Map<String, Object> getVariables() {
        variables.putAll(numberVariables);
        variables.putAll(booleanVariables);
        variables.putAll(stringVariables);
        return variables;
    }

    public void addBlock(int x, int y, int z, String blockID, boolean replace) {
        structureScript.append(String.format("block(%d, %d, %d, \"%s\", %b);\n", x, y, z, blockID, replace));
    }

    public void addShape(MathEquation equation, Vector3 relativePosition, MaterialPalette materialPalette, boolean fill) {
        String selectedBlock = materialPalette.selectBlock();

        // Iterate over a 3D grid of blocks (based on the size/area of the shape)
        for (int x = -relativePosition.x; x <= relativePosition.x; x++) {
            for (int y = -relativePosition.y; y <= relativePosition.y; y++) {
                for (int z = -relativePosition.z; z <= relativePosition.z; z++) {
                    // Apply the equation to check if the block should be added
                    if (equation.apply(x, y, z)) {
                        if (fill || isBorder(x, y, z, relativePosition.x)) {
                            addBlock(relativePosition.x + x, relativePosition.y + y, relativePosition.z + z, selectedBlock, false);
                        }
                    }
                }
            }
        }
    }

    public void addSphere(int radius, Vector3 relativePosition, String block, boolean fill) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.sqrt(x * x + y * y + z * z) <= radius) {
                        if (fill || isBorder(x, y, z, radius)) {
                            addBlock(relativePosition.x + x, relativePosition.y + y, relativePosition.z + z, block, false);
                        }
                    }
                }
            }
        }
    }

    public void addCone(int radius, int height, Vector3 relativePosition, MaterialPalette materialPalette, boolean fill) {
        for (int y = 0; y < height; y++) {
            int currentRadius = (int) (radius * ((height - y) / (float) height));
            addCircle(currentRadius, new Vector3(relativePosition.x, relativePosition.y + y, relativePosition.z), materialPalette, fill);
        }
    }

    public void addCircle(int radius, Vector3 relativePosition, MaterialPalette materialPalette, boolean fill) {
        String selectedBlock = materialPalette.selectBlock();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (Math.sqrt(x * x + z * z) <= radius) {
                    if (fill || isBorder(x, 0, z, radius)) {
                        addBlock(relativePosition.x + x, relativePosition.y, relativePosition.z + z, selectedBlock, false);
                    }
                }
            }
        }
    }

    private boolean isBorder(int x, int y, int z, int size) {
        return x == 0 || y == 0 || z == 0 || x == size - 1 || y == size - 1 || z == size - 1;
    }

    public String generateStructureScript() {
        return structureScript.toString();
    }

    // Enum to represent 3D Directions
    public enum Direction {
        N,
        S,
        E,
        W,
        U,
        D  // North, South, East, West, Up, Down
    }
    public enum VariableType {
        STRING,
        INTEGER,
        BOOLEAN,
    }

    // MaterialPalette Class to hold material distribution for 3D directions
    public static class MaterialPalette {
        private final Direction direction;
        private final Map<String, Integer> materialMap;

        public MaterialPalette(Direction direction, Map<String, Integer> materialMap) {
            this.direction = direction;
            this.materialMap = materialMap;
        }

        // Method to select a block based on the material map's distribution
        public String selectBlock() {
            int totalPercentage = 0;
            for (Integer value : materialMap.values()) {
                totalPercentage += value;
            }

            Random rand = new Random();
            int randomValue = rand.nextInt(totalPercentage);

            int cumulativePercentage = 0;
            for (Map.Entry<String, Integer> entry : materialMap.entrySet()) {
                cumulativePercentage += entry.getValue();
                if (randomValue < cumulativePercentage) {
                    return entry.getKey();
                }
            }

            return null; // Fallback, should not reach here if the materialMap is valid
        }

        // Getter for direction
        public Direction getDirection() {
            return direction;
        }
    }

    public static abstract class MathEquation {
        public abstract boolean apply(int x, int y, int z);
    }

}
