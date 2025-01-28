package org.vicky.vspe.utilities.DatabaseManager.utils;

public enum Hbm2DdlAutoType {
    NONE,
    VALIDATE,
    UPDATE,
    CREATE,
    CREATE_DROP,
    DROP,
    CREATE_ONLY;

    @Override
    public String toString() {
        return name().toLowerCase().replace("_", "-");
    }
}

