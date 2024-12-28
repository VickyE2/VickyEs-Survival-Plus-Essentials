package org.vicky.vspe.utilities.DatabaseManager;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.vicky.vspe.VSPE;

import static org.vicky.vspe.utilities.DatabaseManager.HibernateUtil.sessionFactory;

public class HibernateDatabaseManager {

    // Save an entity
    public <T> void saveEntity(T entity) {
        Transaction transaction = null;
        Session session = sessionFactory.openSession();
        try {
            transaction = session.beginTransaction();
            session.merge(entity);
            transaction.commit();
        } catch (Exception e) {
            VSPE.getInstancedLogger().severe("Error during entity saving: " + e);
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            throw new RuntimeException("Error during entity saving: " + e);
        } finally {
            session.clear();
            session.close();
        }
    }

    // Get an entity by ID
    public <T> T getEntityById(Class<T> clazz, Object id) {
        Session session = sessionFactory.openSession();
        try {
            return session.get(clazz, id);
        } catch (Exception e) {
            throw new RuntimeException("Error during entity saving: " + e);
        } finally {
            session.close();
        }
    }

    public <T> T getEntityByNaturalId(Class<T> clazz, Object id) {
        Session session = sessionFactory.openSession();
        try {
            return session.byNaturalId(clazz)
                    .using("key", id)
                    .load();
        } catch (Exception e) {
            throw new RuntimeException("Error during entity saving: " + e);
        } finally {
            session.close();
        }
    }

    // Update an entity
    public <T> void updateEntity(T entity) {
        Transaction transaction = null;
        Session session = sessionFactory.openSession();
        try {
            transaction = session.beginTransaction();
            session.update(entity);
            transaction.commit();
        } catch (Exception e) {
            VSPE.getInstancedLogger().severe("Error during entity updating:" + e);
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            throw new RuntimeException("Error during entity saving: " + e);
        } finally {
            session.close();
        }
    }

    public <T> void saveOrUpdate(T entity) {
        Transaction transaction = null;
        Session session = sessionFactory.openSession();
        try {
            transaction = session.beginTransaction();
            session.saveOrUpdate(entity);
            transaction.commit();
        } catch (Exception e) {
            VSPE.getInstancedLogger().severe("Error during entity updating:" + e);
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            throw new RuntimeException("Error during entity saving: " + e);
        } finally {
            session.close();
        }
    }

    // Check if an entity exists by ID
    public <T> boolean entityExists(Class<T> clazz, Object id) {
        Session session = sessionFactory.openSession();
        try {
            String entityName = clazz.getSimpleName();
            String queryString = "SELECT COUNT(e) FROM " + entityName + " e WHERE e.id = :id";
            Long count = session.createQuery(queryString, Long.class)
                    .setParameter("id", id)
                    .uniqueResult();
            return count != null && count > 0;
        } catch (Exception e) {
            VSPE.getInstancedLogger().severe("Error during entity query: " + e);
            e.printStackTrace();
            return false;
        } finally {
            session.close();
        }
    }

    public <T> boolean entityExistsByNaturalId(Class<T> clazz, String naturalIdProperty, Object naturalIdValue) {
        Session session = sessionFactory.openSession();
        try {
            // Start a transaction to use NaturalId API
            session.beginTransaction();

            // Use Hibernate's natural ID query
            T result = session.byNaturalId(clazz)
                    .using(naturalIdProperty, naturalIdValue)
                    .load();

            // Commit transaction
            session.getTransaction().commit();

            // Check if entity exists
            return result != null;

        } catch (Exception e) {
            VSPE.getInstancedLogger().severe("Error during natural ID query: " + e);
            e.printStackTrace();
            return false;
        } finally {
            session.close();
        }
    }


    // Delete an entity
    public <T> void deleteEntity(T entity) {
        Transaction transaction = null;
        Session session = sessionFactory.openSession();
        try {
            transaction = session.beginTransaction();
            session.delete(entity);
            transaction.commit();
        } catch (Exception e) {
            VSPE.getInstancedLogger().severe("Error during entity deletion: " + e);
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

}
