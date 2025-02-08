package org.vicky.vspe.utilities.api;
import org.vicky.vspe.utilities.DBTemplates.Advancement;
import org.vicky.vspe.utilities.dao_s.AdvancementDAO;

import java.util.List;

public class AdvancementService {
    private static AdvancementService instance;
    private final AdvancementDAO dao = new AdvancementDAO();

    // Singleton pattern
    private AdvancementService() {}

    public static synchronized AdvancementService getInstance() {
        if (instance == null) {
            instance = new AdvancementService();
        }
        return instance;
    }

    public Advancement createAdvancement(Advancement advancement) {
        return dao.create(advancement);
    }

    public Advancement getAdvancementById(String id) {
        return dao.findById(id);
    }

    public List<Advancement> getAllAdvancements() {
        return dao.findAll();
    }

    public Advancement updateAdvancement(Advancement advancement) {
        return dao.update(advancement);
    }

    public void deleteAdvancement(Advancement advancement) {
        dao.delete(advancement);
    }
}