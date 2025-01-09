package org.vicky.vspe.utilities.DatabaseManager;

import org.bukkit.plugin.java.JavaPlugin;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.vicky.utilities.ANSIColor;
import org.vicky.vspe.utilities.DatabaseManager.templates.Advancement;
import org.vicky.vspe.utilities.DatabaseManager.templates.AdvancementManager;
import org.vicky.vspe.utilities.DatabaseManager.templates.DatabasePlayer;
import org.vicky.vspe.utilities.RandomStringGenerator;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class SQLManager {

    static RandomStringGenerator generator = RandomStringGenerator.getInstance();

    private static String userName;
    private static String password;

    private static SessionFactory sessionFactory;
    private static String hibernateConfig = "";
    private static boolean isSqlite;
    private static File sqliteFile;
    private static String dialect;
    private static DatabaseCreator database;

    private static JavaPlugin plugin;  // Changed to non-static


    // Constructor with plugin instance
    public SQLManager() {

    }

    // Load database credentials from file
    public static Properties loadCredentials(JavaPlugin plugin) {
        Properties properties = new Properties();
        try (FileReader reader = new FileReader(new File(plugin.getDataFolder(), "configs/db_credentials.properties"))) {
            properties.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    // Configure Hibernate session factory
    public static SessionFactory configureSessionFactory(
            String jdbcUrl,
            String username,
            String password,
            String dialect,
            boolean showSql,
            boolean formatSql,
            String ddlAuto
    ) {
        try {
            Configuration configuration = new Configuration();

            // Database settings
            configuration.setProperty("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver"); // Change for other DBs
            configuration.setProperty("hibernate.connection.url", jdbcUrl);

            if (username != null && password != null) {
                configuration.setProperty("hibernate.connection.username", username);
                configuration.setProperty("hibernate.connection.password", password);
            }

            // Hibernate settings
            configuration.setProperty("hibernate.dialect", dialect);
            configuration.setProperty("hibernate.show_sql", String.valueOf(showSql));
            configuration.setProperty("hibernate.format_sql", String.valueOf(formatSql));
            configuration.setProperty("hibernate.hbm2ddl.auto", ddlAuto);

            // Add annotated classes
            configuration.addAnnotatedClass(DatabasePlayer.class);
            configuration.addAnnotatedClass(AdvancementManager.class);
            configuration.addAnnotatedClass(Advancement.class);

            sessionFactory = configuration.buildSessionFactory();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error creating Hibernate SessionFactory", e);
        }
        return sessionFactory;
    }

    // Generate Hibernate configuration
    private static String generateHibernateConfig(DatabaseCreator database, String dialect) {
        StringBuilder config = new StringBuilder();

        config.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
                .append("<!DOCTYPE hibernate-configuration PUBLIC\n")
                .append("        \"-//Hibernate/Hibernate Configuration DTD 3.0//EN\"\n")
                .append("        \"http://hibernate.sourceforge.net/hibernate-configuration-3.0.dtd\">\n")
                .append("<hibernate-configuration>\n")
                .append("    <session-factory>\n")
                .append("                                                                   <!-- Database connection settings -->\n")
                .append("        <property name=\"hibernate.connection.driver_class\">").append("org.sqlite.JDBC").append("</property>\n")
                .append("        <property name=\"hibernate.connection.url\">").append(database.getJdbcUrl()).append("</property>\n");

        if (DatabaseCreator.getUser() != null && DatabaseCreator.getPassword() != null) {
            config.append("        <property name=\"hibernate.connection.username\">").append(DatabaseCreator.getUser()).append("</property>\n")
                    .append("        <property name=\"hibernate.connection.password\">").append(DatabaseCreator.getPassword()).append("</property>\n");
        }

        config.append("                                              <!-- Hibernate settings -->\n")
                .append("        <property name=\"hibernate.dialect\">").append(dialect).append("</property>\n")
                .append("        <property name=\"hibernate.show_sql\">true</property>\n")
                .append("        <property name=\"hibernate.format_sql\">true</property>\n")
                .append("        <property name=\"hibernate.hbm2ddl.auto\">update</property>\n")
                .append("        <property name=\"hibernate.jdbc.fetch_size\">50</property>\n")
                .append("        <property name=\"org.hibernate.SQL\">DEBUG</property>\n")
                .append("        <property name=\"org.hibernate.type.descriptor.sql.BasicBinder\">TRACE</property>\n")
                .append("""
                                                               <!-- Mappings -->
                                <mapping class="org.vicky.vspe.utilities.DatabaseManager.templates.DatabasePlayer"/>
                                <mapping class="org.vicky.vspe.utilities.DatabaseManager.templates.AdvancementManager"/>
                                <mapping class="org.vicky.vspe.utilities.DatabaseManager.templates.Advancement"/>
                        """)
                .append("    </session-factory>\n")
                .append("</hibernate-configuration>\n");

        return config.toString();
    }

    // Write Hibernate config to file
    private static File writeHibernateConfigToFile(String configContent, String filePath) {
        try {
            // Create a File object from the file path
            File file = new File(filePath);

            // Ensure the parent directory exists
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (parentDir.mkdirs()) {
                    plugin.getLogger().info("Created directories: " + parentDir.getAbsolutePath());
                } else {
                    throw new IOException("Failed to create directories: " + parentDir.getAbsolutePath());
                }
            }

            // Write the file
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(configContent);
                plugin.getLogger().info(ANSIColor.colorize("green[Hibernate configuration file generated successfully at: ]" + filePath));

                return file;
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to write Hibernate configuration file: " + e.getMessage());
            return null;
        }
    }

    // Static method to load credentials after plugin is initialized
    public void initialize(JavaPlugin instancedPlugin) {
        plugin = instancedPlugin;
        Properties dbCredentials = loadCredentials(plugin);
        userName = dbCredentials.getProperty("userName", generator.generate(20, true, true, true, false));
        password = dbCredentials.getProperty("password", generator.generatePassword(30));
    }

    // Create the database and configure settings
    public void createDatabase() throws Exception {
        saveCredentials(userName, password);

        // Ensure the plugin data folder exists
        File parentFolder = new File(plugin.getDataFolder(), "databases");
        parentFolder.mkdirs();  // Ensure the folder exists

        // Use the data folder for SQLite database file
        String sqlitePath = new File(parentFolder, "global.db").getAbsolutePath();
        database = new DatabaseCreator.DatabaseBuilder()
                .plugin(plugin)
                .name(sqlitePath)
                .build();

        database.createDatabase();

        isSqlite = true;
        sqliteFile = new File(sqlitePath);
        dialect = "org.hibernate.community.dialect.SQLiteDialect";
        hibernateConfig = generateHibernateConfig(
                database,
                dialect
        );

        // Initialize Hibernate with generated config
        HibernateUtil.initialise(
                writeHibernateConfigToFile(generateHibernateConfig(database, dialect), "./plugins/VickyEs_Survival_Plus_Essentials/configs/hibernate.cfg.xml")
                , plugin
        );
    }

    // Save database credentials to a file
    public void saveCredentials(String userName, String password) {
        try (FileWriter writer = new FileWriter("./plugins/VickyEs_Survival_Plus_Essentials/configs/db_credentials.properties")) {
            Properties properties = new Properties();
            properties.setProperty("userName", userName);
            properties.setProperty("password", password);
            properties.store(writer, "Database Credentials");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
