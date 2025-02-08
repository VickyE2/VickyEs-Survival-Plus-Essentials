package org.vicky.vspe.utilities.api;
import org.vicky.vspe.utilities.DBTemplates.AdvanceablePlayer;
import org.vicky.vspe.utilities.dao_s.AdvanceablePlayerDAO;

import java.util.List;

public class AdvanceablePlayerService {
    private static AdvanceablePlayerService instance;
    private final AdvanceablePlayerDAO dao = new AdvanceablePlayerDAO();

    // Private constructor for singleton pattern
    private AdvanceablePlayerService() {}

    public static synchronized AdvanceablePlayerService getInstance() {
        if (instance == null) {
            instance = new AdvanceablePlayerService();
        }
        return instance;
    }

    public AdvanceablePlayer createPlayer(AdvanceablePlayer player) {
        return dao.create(player);
    }

    public AdvanceablePlayer getPlayerById(Long id) {
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