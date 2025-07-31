package org.vicky.vspe.systems.dimension.Generator.utils.Biome.extend;

public class BaseExtendibles {
    private final String id;
    private String terrainData;

    public BaseExtendibles(String id) {
        this.id = id;
        this.terrainData = "";
    }

    public String getTerrainData() {
        return this.terrainData;
    }

    public void setTerrainData(String terrainData) {
        this.terrainData = terrainData;
    }

    public String getId() {
        return this.id;
    }
}
