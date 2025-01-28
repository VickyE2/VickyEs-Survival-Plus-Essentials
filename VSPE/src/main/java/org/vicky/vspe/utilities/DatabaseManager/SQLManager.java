package org.vicky.vspe.utilities.DatabaseManager;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.vicky.utilities.ANSIColor;
import org.vicky.vspe.systems.ContextLogger.ContextLogger;
import org.vicky.vspe.utilities.RandomStringGenerator;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SQLManager {
    public static RandomStringGenerator generator = RandomStringGenerator.getInstance();
    private final JavaPlugin plugin;
    private String jdbcUrl;
    private final String username;
    private final String password;
    private String hibernateConfig = "";
    private boolean isSqlite;
    private File sqliteFile;
    private DatabaseCreator database;
    private final String dialect;
    private final boolean showSql;
    private final boolean formatSql;
    private final String ddlAuto;
    private final List<Class<?>> mappingClasses;
    private SessionFactory sessionFactory;
    private ContextLogger logger = new ContextLogger(ContextLogger.ContextType.HIBERNATE, "MANAGER");

    public void addMappingClass(Class<?> clazz) {
        mappingClasses.add(clazz);
    }

    public static Properties loadCredentials(JavaPlugin plugin) {
        Properties properties = new Properties();

        try (FileReader reader = new FileReader(new File(plugin.getDataFolder(), "configs/db_credentials.properties"))) {
            properties.load(reader);
        } catch (IOException var7) {
            var7.printStackTrace();
        }

        return properties;
    }

    public SQLManager(JavaPlugin plugin, String username, String password, String dialect,
                      boolean showSql, boolean formatSql, String ddlAuto, List<Class<?>> mappingClasses) {
        this.plugin = plugin;
        Properties dbCredentials = loadCredentials(plugin);
        this.username = dbCredentials.getOrDefault("username", username).toString();
        this.password = dbCredentials.getOrDefault("password", password).toString();
        this.saveCredentials(this.username, this.password);
        this.dialect = dialect;
        this.showSql = showSql;
        this.formatSql = formatSql;
        this.ddlAuto = ddlAuto;
        this.mappingClasses = mappingClasses != null ? new ArrayList<>(mappingClasses) : new ArrayList<>();
        configureSessionFactory();
    }

    private void configureSessionFactory() {
        try {
            Configuration configuration = new Configuration();

            createDatabase();
            configuration.setProperty("hibernate.connection.driver_class", "org.sqlite.JDBC"); // Adjust driver as needed
            configuration.setProperty("hibernate.connection.url", jdbcUrl);
            configuration.setProperty("hibernate.connection.username", username);
            configuration.setProperty("hibernate.connection.password", password);
            configuration.setProperty("hibernate.dialect", dialect);
            configuration.setProperty("org.hibernate.SQL", "ERROR");
            configuration.setProperty("hibernate.show_sql", Boolean.toString(showSql));
            configuration.setProperty("hibernate.format_sql", Boolean.toString(formatSql));
            configuration.setProperty("hibernate.hbm2ddl.auto", ddlAuto);

            for (Class<?> clazz : mappingClasses) {
                configuration.addAnnotatedClass(clazz);
            }

            this.sessionFactory = configuration.buildSessionFactory();
        } catch (Exception e) {
           logger.print("Failed to configure Hibernate SessionFactory: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void createDatabase() throws Exception {
        File parentFolder = new File(plugin.getDataFolder(), "databases");
        parentFolder.mkdirs();
        String sqlitePath = new File(parentFolder, "global.db").getAbsolutePath();
        database = new DatabaseCreator.DatabaseBuilder().name(sqlitePath).build();
        database.createDatabase();
        this.jdbcUrl = database.getJdbcUrl();
    }

    public void startDatabase() {
        try {
            HibernateUtil.initialise(this);
        }catch (Exception e) {
            logger.print(e.getMessage() + ": " + e.getCause(), true);
        }
    }

    public void saveCredentials(String userName, String password) {
        try (FileWriter writer = new FileWriter("./plugins/VickyEs_Survival_Plus_Essentials/configs/db_credentials.properties")) {
            Properties properties = new Properties();
            properties.setProperty("userName", userName);
            properties.setProperty("password", password);
            properties.store(writer, "Database Credentials");
        } catch (IOException var8) {
            var8.printStackTrace();
        }
    }
}
