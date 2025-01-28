package org.vicky.vspe.utilities.DatabaseManager;

import org.bukkit.plugin.java.JavaPlugin;
import org.vicky.utilities.ANSIColor;
import org.vicky.vspe.VSPE;
import org.vicky.vspe.systems.ContextLogger.ContextLogger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseCreator {
    private static String name = "";
    private static String user = "";
    private static String password = "";
    private static String databaseName = "";
    private final String jdbcUrl;
    private ContextLogger logger = new ContextLogger(ContextLogger.ContextType.HIBERNATE, "DB");

    private DatabaseCreator(DatabaseBuilder builder) {
        name = builder.name;
        user = builder.user;
        password = builder.password;
        databaseName = builder.databaseName;
        this.jdbcUrl = "jdbc:sqlite:" + name;
    }

    public static String getDatabaseName() {
        return databaseName;
    }

    public static String getName() {
        return name;
    }

    public static String getPassword() {
        return password;
    }

    public static String getUser() {
        return user;
    }

    public String getJdbcUrl() {
        return this.jdbcUrl;
    }

    public void createDatabase() throws Exception {
        String jdbcUrl = "jdbc:sqlite:" + name;

        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            if (conn != null) {
                logger.print(ANSIColor.colorize("green[Database successfully created and connected @ "+ jdbcUrl + "]"));
            }
        } catch (SQLException var7) {
            logger.print(ANSIColor.colorize("red[" + var7.getMessage() + "]"), true);
        }
    }

    public static class DatabaseBuilder {
        private String name;
        private String user;
        private String password;
        private String databaseName;

        public DatabaseBuilder name(String name) {
            this.name = name;
            return this;
        }

        public DatabaseBuilder user(String user) {
            this.user = user;
            return this;
        }

        public DatabaseBuilder password(String password) {
            this.password = password;
            return this;
        }

        public DatabaseBuilder databaseName(String databaseName) {
            this.databaseName = databaseName;
            return this;
        }

        public DatabaseCreator build() {
            return new DatabaseCreator(this);
        }
    }
}
