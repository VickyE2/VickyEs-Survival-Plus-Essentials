package org.vicky.vspe.utilities.DatabaseManager;

import java.io.File;
import org.bukkit.plugin.java.JavaPlugin;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.vicky.utilities.ANSIColor;
import org.vicky.vspe.VSPE;

public class HibernateUtil {
   public static SessionFactory sessionFactory = null;
   public static JavaPlugin plugin = null;

   public static void initialise(File configFile, JavaPlugin initialisingPlugin) {
      try {
         plugin = initialisingPlugin;
         Thread.sleep(2000L);
         sessionFactory = new Configuration().configure(configFile).buildSessionFactory();
         sessionFactory.openSession();
         VSPE.getInstancedLogger().info(ANSIColor.colorize("green[Session started successfully]"));
      } catch (Throwable var3) {
         plugin.getLogger().severe("Initial SessionFactory creation failed." + var3);
         throw new ExceptionInInitializerError(var3);
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
      } catch (Throwable var4) {
         plugin.getLogger().severe("Initial SessionFactory creation failed." + var4);
         throw new ExceptionInInitializerError(var4);
      }
   }

   public static SessionFactory getSessionFactory() {
      return sessionFactory;
   }

   public static void shutdown() {
      getSessionFactory().close();
   }
}
