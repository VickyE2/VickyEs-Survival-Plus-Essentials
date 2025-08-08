package org.vicky.vspe.platform.utilities.Hibernate.api;

import org.vicky.vspe.platform.utilities.Hibernate.DBTemplates.Advancement;
import org.vicky.vspe.platform.utilities.Hibernate.dao_s.AdvancementDAO;

import java.util.List;
import java.util.Optional;

public class AdvancementService {
    private static AdvancementService instance;
    private final AdvancementDAO dao = new AdvancementDAO();

    // Singleton pattern
    private AdvancementService() {
    }

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

    public Optional<Advancement> getAdvancementByName(String name) {
        return dao.findByName(name);
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