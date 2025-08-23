package org.vicky.vspe.platform.systems.fakeRegistry;

public interface DataSaver {
    void initialize();

    void save();

    void saveData(SavableData data);

    SavableData getData(String id);
}
