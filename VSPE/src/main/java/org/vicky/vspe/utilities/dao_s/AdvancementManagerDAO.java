package org.vicky.vspe.utilities.dao_s;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.vicky.utilities.DatabaseManager.HibernateUtil;
import org.vicky.vspe.utilities.DBTemplates.Advancement;
import org.vicky.vspe.utilities.DBTemplates.AdvancementManager;

import java.util.List;

public class AdvancementManagerDAO {

    public AdvancementManager create(AdvancementManager advancement) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(advancement);
            transaction.commit();
            return advancement;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public AdvancementManager findById(String id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(AdvancementManager.class, id);
        }
    }

    public List<Advancement> findAdvancementsFor(AdvancementManager manager) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            TypedQuery<Advancement> query = em.createQuery(
                    "SELECT f FROM Advancement f WHERE f.manager_id = :managerID", Advancement.class);
            query.setParameter("managerID", manager.getId());
            return query.getResultList();
        }
        finally {
            em.close();
        }
    }

    public AdvancementManager update(AdvancementManager advancement) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(advancement);
            transaction.commit();
            return advancement;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public void delete(AdvancementManager advancement) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.remove(advancement);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            throw e;
        }
    }
}