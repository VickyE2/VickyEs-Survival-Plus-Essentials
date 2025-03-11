package org.vicky.vspe.utilities.Hibernate.dao_s;

import jakarta.persistence.EntityManager;
import org.vicky.utilities.DatabaseManager.HibernateUtil;
import org.vicky.vspe.utilities.Hibernate.DBTemplates.Dimension;

import java.util.List;

/**
 * Data Access Object for managing {@link Dimension} entities.
 * <p>
 * Provides methods to create, find, update, and delete Dimension entities.
 * </p>
 */
public class DimensionDAO {

    /**
     * Persists a new Dimension entity.
     *
     * @param dimension the Dimension to create
     * @return the created Dimension
     */
    public Dimension create(Dimension dimension) {
        try (EntityManager em = HibernateUtil.getEntityManager()) {
            em.getTransaction().begin();
            em.persist(dimension);
            em.getTransaction().commit();
            return dimension;
        }
    }

    /**
     * Finds a Dimension by its identifier.
     *
     * @param id the identifier of the Dimension
     * @return the found Dimension, or null if not found
     */
    public Dimension findById(String id) {
        try (EntityManager em = HibernateUtil.getEntityManager()) {
            return em.find(Dimension.class, id);
        }
    }

    /**
     * Retrieves all Dimension entities.
     *
     * @return a list of all Dimension entities
     */
    public List<Dimension> findAll() {
        try (EntityManager em = HibernateUtil.getEntityManager()) {
            return em.createQuery("from Dimension", Dimension.class).getResultList();
        }
    }

    /**
     * Updates an existing Dimension entity.
     *
     * @param dimension the Dimension to update
     * @return the updated Dimension
     */
    public Dimension update(Dimension dimension) {
        try (EntityManager em = HibernateUtil.getEntityManager()) {
            em.getTransaction().begin();
            dimension = em.merge(dimension);
            em.getTransaction().commit();
            return dimension;
        }
    }

    /**
     * Deletes an existing Dimension entity.
     *
     * @param dimension the Dimension to delete
     */
    public void delete(Dimension dimension) {
        try (EntityManager em = HibernateUtil.getEntityManager()) {
            em.getTransaction().begin();
            Dimension found = em.find(Dimension.class, dimension.getId());
            if (found != null) {
                em.remove(found);
            }
            em.getTransaction().commit();
        }
    }
}
