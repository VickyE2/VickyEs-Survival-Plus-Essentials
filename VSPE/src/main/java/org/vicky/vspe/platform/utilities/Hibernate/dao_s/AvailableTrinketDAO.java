package org.vicky.vspe.platform.utilities.Hibernate.dao_s;

import org.vicky.shaded.jakarta.persistence.EntityManager;
import org.vicky.utilities.DatabaseManager.HibernateUtil;
import org.vicky.vspe.platform.utilities.Hibernate.DBTemplates.AvailableTrinket;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object (DAO) for the {@link AvailableTrinket} entity.
 * <p>
 * This class provides methods to create, read, update, and delete AvailableTrinket records
 * in the database. It uses Hibernate and a utility class {@code HibernateUtil} to manage the
 * EntityManager.
 * </p>
 */
public class AvailableTrinketDAO {

    /**
     * Persists a new AvailableTrinket entity to the database.
     *
     * @param trinket the AvailableTrinket entity to save
     */
    public void save(AvailableTrinket trinket) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(trinket);
            em.getTransaction().commit();
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
     * Retrieves an AvailableTrinket entity by its identifier.
     *
     * @param id the ID of the AvailableTrinket to find
     * @return an Optional containing the AvailableTrinket if found, or empty if not found
     */
    public Optional<AvailableTrinket> findById(String id) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            AvailableTrinket trinket = em.find(AvailableTrinket.class, id);
            return Optional.ofNullable(trinket);
        } finally {
            em.close();
        }
    }

    /**
     * Retrieves all AvailableTrinket entities from the database.
     *
     * @return a List of AvailableTrinket entities
     */
    public List<AvailableTrinket> findAll() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery("SELECT t FROM AvailableTrinket t", AvailableTrinket.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Updates an existing AvailableTrinket entity in the database.
     *
     * @param trinket the AvailableTrinket entity with updated data
     * @return the merged (updated) AvailableTrinket entity
     */
    public AvailableTrinket update(AvailableTrinket trinket) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            AvailableTrinket updated = em.merge(trinket);
            em.getTransaction().commit();
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
     * Deletes the given AvailableTrinket entity from the database.
     *
     * @param trinket the AvailableTrinket entity to delete
     */
    public void delete(AvailableTrinket trinket) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            AvailableTrinket attached = em.find(AvailableTrinket.class, trinket.getId());
            if (attached != null) {
                em.remove(attached);
            }
            em.getTransaction().commit();
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
     * Creates and persists a new AvailableTrinket entity with the specified identifier and name.
     * <p>
     * This is a convenience method for creating a new AvailableTrinket without having to manually
     * instantiate the object and then call save().
     * </p>
     *
     * @param id   the identifier for the new AvailableTrinket (as a String)
     * @param name the name for the new AvailableTrinket
     * @return the newly created and persisted AvailableTrinket entity
     */
    public AvailableTrinket create(String id, String name) {
        AvailableTrinket trinket = new AvailableTrinket();
        trinket.setId(id);
        trinket.setName(name);
        save(trinket);
        return trinket;
    }
}
