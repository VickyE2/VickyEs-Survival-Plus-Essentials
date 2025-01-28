package org.vicky.vspe.utilities.DatabaseManager;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.vicky.utilities.ANSIColor;
import org.vicky.vspe.VSPE;
import org.vicky.vspe.systems.ContextLogger.ContextLogger;

import static org.vicky.vspe.utilities.global.GlobalResources.configManager;

public class CustomStatementInspector implements StatementInspector {
    private ContextLogger logger = new ContextLogger(ContextLogger.ContextType.HIBERNATE, "LOG");

    @Override
    public String inspect(String sql) {
        if (sql.trim().toLowerCase().startsWith("select")) {
            logSelectQuery(sql);
        }
        return sql; // Return the query unmodified
    }

    private void logSelectQuery(String sql) {
        // Extract entity name and ID from the SQL query (simple example)
        String entityName = "Unknown Entity";
        String entityId = "Unknown ID";

        if (sql.contains("from")) {
            String[] parts = sql.split("from");
            String tableName = parts[1].split(" ")[1].trim();
            entityName = tableName.substring(tableName.indexOf('_') + 1); // Adjust based on your table naming convention
        }

        if (sql.contains("where")) {
            String[] conditions = sql.split("where")[1].split("=");
            entityId = conditions[1].trim();
        }

        if (configManager.getBooleanValue("Debug"))
            logger.print(String.format("Selected ~ %s having Id -> %s)", entityName, entityId));
    }
}

