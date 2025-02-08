package org.vicky.vspe.utilities.dao_s;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.vicky.vspe.utilities.DBTemplates.Dimension;
import org.vicky.utilities.DatabaseManager.HibernateUtil; // adjust this import as needed

import java.util.List;

public class DimensionDAO {

    public Dimension create(Dimension dimension) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(dimension);
            transaction.commit();
            return dimension;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public Dimension findById(String id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Dimension.class, id);
        }
    }

    public List<Dimension> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Dimension", Dimension.class).list();
        }
    }

    public Dimension update(Dimension dimension) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(dimension);
            transaction.commit();
            return dimension;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public void delete(Dimension dimension) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.remove(dimension);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            throw e;
        }
    }
}