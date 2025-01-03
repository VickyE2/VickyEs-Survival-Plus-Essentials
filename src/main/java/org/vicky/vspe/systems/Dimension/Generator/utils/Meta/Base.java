package org.vicky.vspe.systems.Dimension.Generator.utils.Meta;

import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.DepositableFeature;
import org.vicky.vspe.systems.Dimension.Generator.utils.GlobalPreprocessor;
import org.vicky.vspe.systems.Dimension.Generator.utils.Structures.DepositableStructure;

import java.util.ArrayList;
import java.util.List;

public class Base {
    private final List<GlobalPreprocessor> enabledPreProcessor;
    private final List<DepositableFeature> defaultDeposits;
    private final List<DepositableFeature> defaultOres;
    private final List<DepositableStructure> CUSTOM_DEPOSITABLE_STRUCTURES = new ArrayList<>();
    private int slantDepth;
    private int oceanLevel;


    public Base() {
        this.slantDepth = 15;
        this.oceanLevel = 70;
        this.enabledPreProcessor = new ArrayList<>();
        this.defaultDeposits = new ArrayList<>();
        this.defaultOres = new ArrayList<>();
    }

    public void addPreprocessor(GlobalPreprocessor preprocessor) {
        this.enabledPreProcessor.add(preprocessor);
    }

    public void addDeposit(DepositableFeature feature) {
        this.defaultDeposits.add(feature);
    }

    public void addDeposit(DepositableFeature... features) {
        this.defaultDeposits.addAll(List.of(features));
    }

    public void addPreprocessor(DepositableFeature depositableFeature) {
        this.defaultOres.add(depositableFeature);
    }

    public int getOceanLevel() {
        return oceanLevel;
    }

    public void setOceanLevel(int oceanLevel) {
        this.oceanLevel = oceanLevel;
    }

    public int getSlantDepth() {
        return slantDepth;
    }

    public void setSlantDepth(int slantDepth) {
        this.slantDepth = slantDepth;
    }

    public List<DepositableFeature> getDefaultDeposits() {
        return defaultDeposits;
    }

    public List<GlobalPreprocessor> getEnabledPreProcessor() {
        return enabledPreProcessor;
    }

    public List<DepositableFeature> getDefaultOres() {
        return defaultOres;
    }

    public List<DepositableStructure> getDefaultDepositStructures() {
        List<DepositableStructure> depositableStructures = new ArrayList<>();

        for (DepositableFeature feature : defaultDeposits) {
            for (DepositableStructure actualStructure : feature.anchorStructures)
                if (depositableStructures.stream().noneMatch(depositableStructure -> depositableStructure.getClass() == actualStructure.getClass()))
                    depositableStructures.add(actualStructure);
        }

        return depositableStructures;
    }

    public List<DepositableStructure> getDefaultOreStructures() {
        List<DepositableStructure> depositableStructures = new ArrayList<>();

        for (DepositableFeature feature : defaultOres) {
            for (DepositableStructure actualStructure : feature.anchorStructures)
                if (depositableStructures.stream().noneMatch(depositableStructure -> depositableStructure.getClass() == actualStructure.getClass()))
                    depositableStructures.add(actualStructure);
        }

        return depositableStructures;
    }
}