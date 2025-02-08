package org.vicky.vspe.utilities.dao_s;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.vicky.vspe.utilities.DBTemplates.AdvanceablePlayer;
import org.vicky.utilities.DatabaseManager.HibernateUtil; // Adjust this import to your utility

import java.util.List;

public class AdvanceablePlayerDAO {

    public AdvanceablePlayer create(AdvanceablePlayer player) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(player);
            transaction.commit();
            return player;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public AdvanceablePlayer findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(AdvanceablePlayer.class, id);
        }
    }

    public List<AdvanceablePlayer> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from AdvanceablePlayer", AdvanceablePlayer.class).list();
        }
    }

    public AdvanceablePlayer update(AdvanceablePlayer player) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(player);
            transaction.commit();
            return player;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public void delete(AdvanceablePlayer player) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.remove(player);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            throw e;
        }
    }
}