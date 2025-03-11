package org.vicky.vspe.utilities.Hibernate.dao_s;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.vicky.utilities.DatabaseManager.HibernateUtil;
import org.vicky.vspe.utilities.Hibernate.DBTemplates.AdvanceablePlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AdvanceablePlayerDAO {

    /**
     * Persists a new AdvanceablePlayer entity.
     *
     * @param player the AdvanceablePlayer to create
     * @return the created AdvanceablePlayer
     */
    public AdvanceablePlayer create(AdvanceablePlayer player) {
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
            throw new RuntimeException("Error creating AdvanceablePlayer", e);
        } finally {
            em.close();
        }
    }

    /**
     * Retrieves an AdvanceablePlayer by its primary key and eagerly fetches its lazy collection.
     *
     * @param id the UUID of the AdvanceablePlayer.
     * @return an Optional containing the AdvanceablePlayer with its accomplishedAdvancements initialized.
     */
    public Optional<AdvanceablePlayer> findById(UUID id) {
        try (EntityManager em = HibernateUtil.getEntityManager()) {
            // Adjust the field name below ("accomplishedAdvancements") if it differs in your entity.
            TypedQuery<AdvanceablePlayer> query = em.createQuery(
                    "SELECT p FROM AdvanceablePlayer p LEFT JOIN FETCH p.advancements WHERE p.id = :id",
                    AdvanceablePlayer.class);
            query.setParameter("id", id.toString());
            AdvanceablePlayer player = query.getSingleResult();
            return Optional.ofNullable(player);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Retrieves an AdvanceablePlayer by its associated DatabasePlayer's id.
     *
     * @param databasePlayerId the id of the associated DatabasePlayer.
     * @return an Optional containing the AdvanceablePlayer, or empty if not found.
     */
    public Optional<AdvanceablePlayer> findByDatabasePlayerId(String databasePlayerId) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            TypedQuery<AdvanceablePlayer> query = em.createQuery(
                    "SELECT a FROM AdvanceablePlayer a LEFT JOIN FETCH a.accomplishedAdvancements WHERE a.databasePlayer.id = :dbId",
                    AdvanceablePlayer.class);
            query.setParameter("dbId", databasePlayerId);
            return Optional.ofNullable(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }

    /**
     * Retrieves all AdvanceablePlayer entities.
     *
     * @return a list of all AdvanceablePlayer entities.
     */
    public List<AdvanceablePlayer> findAll() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery("SELECT a FROM AdvanceablePlayer a", AdvanceablePlayer.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Updates an existing AdvanceablePlayer entity.
     *
     * @param player the AdvanceablePlayer to update.
     * @return the updated AdvanceablePlayer.
     */
    public AdvanceablePlayer update(AdvanceablePlayer player) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            AdvanceablePlayer updated = em.merge(player);
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
     * Deletes an existing AdvanceablePlayer entity.
     *
     * @param player the AdvanceablePlayer to delete.
     */
    public void delete(AdvanceablePlayer player) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            AdvanceablePlayer attached = em.contains(player) ? player : em.merge(player);
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
