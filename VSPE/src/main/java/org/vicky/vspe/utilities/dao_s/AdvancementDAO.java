package org.vicky.vspe.utilities.dao_s;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.vicky.vspe.utilities.DBTemplates.Advancement;
import org.vicky.utilities.DatabaseManager.HibernateUtil; // adjust this import as needed

import java.util.List;

public class AdvancementDAO {

    public Advancement create(Advancement advancement) {
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

    public Advancement findById(String id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Advancement.class, id);
        }
    }

    public List<Advancement> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Advancement", Advancement.class).list();
        }
    }

    public Advancement update(Advancement advancement) {
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

    public void delete(Advancement advancement) {
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