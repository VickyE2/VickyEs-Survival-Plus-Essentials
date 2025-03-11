package org.vicky.vspe.utilities.Hibernate.dao_s;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.vicky.utilities.DatabaseManager.HibernateUtil;
import org.vicky.vspe.utilities.Hibernate.DBTemplates.Advancement;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for managing {@link Advancement} entities.
 * <p>
 * Provides CRUD operations for Advancement entities.
 * </p>
 */
public class AdvancementDAO {

    /**
     * Persists a new Advancement entity.
     *
     * @param advancement the Advancement to create
     * @return the created Advancement
     */
    public Advancement create(Advancement advancement) {
        try (EntityManager em = HibernateUtil.getEntityManager()) {
            em.getTransaction().begin();
            em.persist(advancement);
            em.getTransaction().commit();
            return advancement;
        }
    }

    /**
     * Finds an Advancement by its identifier.
     *
     * @param id the identifier of the Advancement
     * @return the found Advancement, or null if not found
     */
    public Advancement findById(String id) {
        try (EntityManager em = HibernateUtil.getEntityManager()) {
            return em.find(Advancement.class, id);
        }
        catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Retrieves all Advancement entities.
     *
     * @return a list of all Advancement entities
     */
    public List<Advancement> findAll() {
        try (EntityManager em = HibernateUtil.getEntityManager()) {
            return em.createQuery("from Advancement", Advancement.class).getResultList();
        }
        catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Updates an existing Advancement entity.
     *
     * @param advancement the Advancement to update
     * @return the updated Advancement
     */
    public Advancement update(Advancement advancement) {
        try (EntityManager em = HibernateUtil.getEntityManager()) {
            em.getTransaction().begin();
            advancement = em.merge(advancement);
            em.getTransaction().commit();
            return advancement;
        }
    }

    /**
     * Deletes an existing Advancement entity.
     *
     * @param advancement the Advancement to delete
     */
    public void delete(Advancement advancement) {
        try (EntityManager em = HibernateUtil.getEntityManager()) {
            em.getTransaction().begin();
            Advancement found = em.find(Advancement.class, advancement.getId());
            if (found != null) {
                em.remove(found);
            }
            em.getTransaction().commit();
        }
    }

    public Optional<Advancement> findByName(String name) {
        try (EntityManager em = HibernateUtil.getEntityManager()) {
            TypedQuery<Advancement> query = em.createQuery(
                    "SELECT m FROM Advancement m WHERE m.name = :name",
                    Advancement.class
            );
            query.setParameter("name", name);
            try {
                return Optional.of(query.getSingleResult());
            } catch (NoResultException e) {
                return Optional.empty();
            }
        }
    }
}
