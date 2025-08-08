package org.vicky.vspe.platform.utilities.Hibernate.api;

import org.vicky.vspe.platform.utilities.Hibernate.DBTemplates.AdvanceablePlayer;
import org.vicky.vspe.platform.utilities.Hibernate.dao_s.AdvanceablePlayerDAO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AdvanceablePlayerService {
    private static AdvanceablePlayerService instance;
    private final AdvanceablePlayerDAO dao = new AdvanceablePlayerDAO();

    // Private constructor for singleton pattern
    private AdvanceablePlayerService() {
    }

    public static synchronized AdvanceablePlayerService getInstance() {
        if (instance == null) {
            instance = new AdvanceablePlayerService();
        }
        return instance;
    }

    public AdvanceablePlayer createPlayer(AdvanceablePlayer player) {
        return dao.create(player);
    }

    public Optional<AdvanceablePlayer> getPlayerById(UUID id) {
        return dao.findById(id);
    }

    public List<AdvanceablePlayer> getAllPlayers() {
        return dao.findAll();
    }

    public AdvanceablePlayer updatePlayer(AdvanceablePlayer player) {
        return dao.update(player);
    }

    public void deletePlayer(AdvanceablePlayer player) {
        dao.delete(player);
    }
}