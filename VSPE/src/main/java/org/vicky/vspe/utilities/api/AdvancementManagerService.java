package org.vicky.vspe.utilities.api;
import org.vicky.vspe.utilities.DBTemplates.Advancement;
import org.vicky.vspe.utilities.DBTemplates.AdvancementManager;
import org.vicky.vspe.utilities.dao_s.AdvancementDAO;
import org.vicky.vspe.utilities.dao_s.AdvancementManagerDAO;

import java.util.List;

public class AdvancementManagerService {
    private static AdvancementManagerService instance;
    private final AdvancementManagerDAO dao = new AdvancementManagerDAO();

    // Singleton pattern
    private AdvancementManagerService() {}

    public static synchronized AdvancementManagerService getInstance() {
        if (instance == null) {
            instance = new AdvancementManagerService();
        }
        return instance;
    }

    public AdvancementManager createAdvancementManager(AdvancementManager advancement) {
        return dao.create(advancement);
    }

    public AdvancementManager getAdvancementManagerById(String id) {
        return dao.findById(id);
    }

    public List<Advancement> getAllAdvancementsFor(AdvancementManager manager) {
        return dao.findAdvancementsFor(manager);
    }

    public AdvancementManager updateAdvancementManager(AdvancementManager advancement) {
        return dao.update(advancement);
    }

    public void deleteAdvancementManager(AdvancementManager advancement) {
        dao.delete(advancement);
    }
}