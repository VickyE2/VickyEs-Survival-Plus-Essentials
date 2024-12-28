package org.vicky.vspe.utilities.DatabaseManager;

import org.bukkit.plugin.java.JavaPlugin;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.vicky.utilities.ANSIColor;
import org.vicky.vspe.VSPE;

import java.io.File;

public class HibernateUtil {
    public static SessionFactory sessionFactory = null;
    public static JavaPlugin plugin = null;

    public static void initialise(File configFile, JavaPlugin initialisingPlugin) {
        try {
            plugin = initialisingPlugin;
            Thread.sleep(2000);
            sessionFactory = new Configuration().configure(configFile).buildSessionFactory();
            sessionFactory.openSession();
            VSPE.getInstancedLogger().info(ANSIColor.colorize("green[Session started successfully]"));
        } catch (Throwable ex) {
            plugin.getLogger().severe("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static void initialise(SessionFactory sessionFactory, boolean isSqlite, File file) {
        try {
            if (isSqlite) {
                if (!file.exists()) {
                    File parentFolder = file.getParentFile();
                    parentFolder.mkdirs();
                }
                file.mkdir();
            }
            sessionFactory.openSession();
            VSPE.getInstancedLogger().info(ANSIColor.colorize("green[Session started successfully]"));
        } catch (Throwable ex) {
            plugin.getLogger().severe("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        // Close caches and connection pools
        getSessionFactory().close();
    }
}

