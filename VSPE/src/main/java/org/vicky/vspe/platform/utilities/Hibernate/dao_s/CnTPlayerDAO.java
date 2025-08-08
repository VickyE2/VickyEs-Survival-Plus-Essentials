package org.vicky.vspe.platform.utilities.Hibernate.dao_s;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.vicky.utilities.DatabaseManager.HibernateUtil;
import org.vicky.vspe.platform.utilities.Hibernate.DBTemplates.CnTPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CnTPlayerDAO {

    /**
     * Persists a new CnTPlayer entity.
     *
     * @param player the CnTPlayer to create.
     * @return the created CnTPlayer.
     */
    public CnTPlayer create(CnTPlayer player) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            em.persist(player);
            tx.commit();
            return player;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error creating CnTPlayer", e);
        } finally {
            em.close();
        }
    }

    /**
     * Finds a CnTPlayer by its primary key.
     *
     * @param id the UUID identifier of the CnTPlayer.
     * @return an Optional containing the found CnTPlayer, or empty if not found.
     */
    public Optional<CnTPlayer> findById(UUID id) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return Optional.ofNullable(em.find(CnTPlayer.class, id.toString()));
        } finally {
            em.close();
        }
    }

    /**
     * Retrieves a CnTPlayer by its associated DatabasePlayer's id.
     *
     * @param databasePlayerId the id of the associated DatabasePlayer.
     * @return an Optional containing the CnTPlayer, or empty if not found.
     */
    public Optional<CnTPlayer> findByDatabasePlayerId(String databasePlayerId) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            TypedQuery<CnTPlayer> query = em.createQuery(
                    "SELECT c FROM CnTPlayer c WHERE c.databasePlayer.id = :dbId",
                    CnTPlayer.class);
            query.setParameter("dbId", databasePlayerId);
            return Optional.ofNullable(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }

    /**
     * Retrieves all CnTPlayer entities.
     *
     * @return a list of all CnTPlayer entities.
     */
    public List<CnTPlayer> findAll() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery("SELECT c FROM CnTPlayer c", CnTPlayer.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Updates an existing CnTPlayer entity.
     *
     * @param player the CnTPlayer to update.
     * @return the updated CnTPlayer.
     */
    public CnTPlayer update(CnTPlayer player) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            CnTPlayer updated = em.merge(player);
            tx.commit();
            return updated;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Deletes a CnTPlayer entity.
     *
     * @param player the CnTPlayer to delete.
     */
    public void delete(CnTPlayer player) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            CnTPlayer attached = em.contains(player) ? player : em.merge(player);
            em.remove(attached);
            tx.commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}
