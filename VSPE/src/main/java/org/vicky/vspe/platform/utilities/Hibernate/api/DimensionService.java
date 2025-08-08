package org.vicky.vspe.platform.utilities.Hibernate.api;

import org.vicky.vspe.platform.utilities.Hibernate.DBTemplates.Dimension;
import org.vicky.vspe.platform.utilities.Hibernate.dao_s.DimensionDAO;

import java.util.List;

public class DimensionService {
    private static DimensionService instance;
    private final DimensionDAO dao = new DimensionDAO();

    // Singleton pattern
    private DimensionService() {
    }

    public static synchronized DimensionService getInstance() {
        if (instance == null) {
            instance = new DimensionService();
        }
        return instance;
    }

    public Dimension createDimension(Dimension dimension) {
        return dao.create(dimension);
    }

    public Dimension getDimensionById(String id) {
        return dao.findById(id);
    }

    public List<Dimension> getAllDimensions() {
        return dao.findAll();
    }

    public Dimension updateDimension(Dimension dimension) {
        return dao.update(dimension);
    }

    public void deleteDimension(Dimension dimension) {
        dao.delete(dimension);
    }
}