package ru.shirk.reports.storage.database;

import lombok.Getter;
import lombok.NonNull;
import ru.shirk.reports.Reports;
import ru.shirk.reports.reports.ReportStatus;
import ru.shirk.reports.storage.configs.Configuration;
import ru.shirk.reports.storage.configs.ConfigurationManager;

import java.io.File;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class DatabaseManager {

    private final Configuration config;
    private final Stack<Connection> freePool = new Stack<>();
    private final Set<Connection> occupiedPool = new HashSet<>();
    @Getter
    private final String baseName;

    public DatabaseManager(@NonNull ConfigurationManager configurationManager) {
        this.config = configurationManager.getConfig("settings.yml");
        baseName = config.c("storage.baseName");
        setup();
    }

    private Connection makeAvailable(Connection conn) throws SQLException {
        if (isConnectionAvailable(conn)) {
            return conn;
        }

        occupiedPool.remove(conn);
        conn.close();

        conn = createNewConnection();
        occupiedPool.add(conn);
        return conn;
    }

    private boolean isConnectionAvailable(Connection conn) {
        try (Statement st = conn.createStatement()) {
            st.executeQuery("select 1");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private Connection createNewConnectionForPool() throws SQLException {
        Connection conn = createNewConnection();
        occupiedPool.add(conn);
        return conn;
    }

    private Connection createNewConnection() throws SQLException {
        Connection conn;
        String databaseUrl;
        if (config.c("storage.type").equalsIgnoreCase("mysql")) {
            databaseUrl = "jdbc:mysql://" + config.c("storage.host") + ":" + config.c("storage.port");
        } else {
            databaseUrl = "jdbc:h2:" + Reports.getInstance().getDataFolder().getAbsolutePath() + File.separator + baseName;
        }
        conn = DriverManager.getConnection(databaseUrl, config.c("storage.user"), config.c("storage.password"));
        return conn;
    }

    private Connection getConnectionFromPool() {
        Connection conn = null;

        if (!freePool.isEmpty()) {
            conn = freePool.pop();
            occupiedPool.add(conn);
        }

        return conn;
    }

    public synchronized Connection getConnection() {
        try {
            Connection conn;

            if (isFull()) {
                throw new SQLException("Exceeded the maximum number of connections");
            }

            conn = getConnectionFromPool();

            if (conn == null) {
                conn = createNewConnectionForPool();
            }

            conn = makeAvailable(conn);
            return conn;
        } catch (SQLException e) {
            Reports.getInstance().getLogger().info("Total connections free: " + freePool.size());
            Reports.getInstance().getLogger().info("Total connections: " + occupiedPool.size());
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public synchronized void returnConnection(Connection conn) {
        try {
            if (conn == null) {
                throw new NullPointerException();
            }
            occupiedPool.remove(conn);
            freePool.push(conn);
        } catch (NullPointerException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private synchronized boolean isFull() {
        return ((freePool.size() == 0) && (freePool.size() + occupiedPool.size() >= config.getFile().getInt("storage.maxConnections")));
    }

    private void setup() {
        Connection conn = getConnection();
        try {
            Statement statement = conn.createStatement();

            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + baseName + "`;");

            statement.execute("USE `" + baseName + "`;");

            statement.execute(
                    "CREATE TABLE IF NOT EXISTS `reports` (" +
                            "`id` INT AUTO_INCREMENT PRIMARY KEY, " +
                            "`username` VARCHAR(20) NOT NULL, " +
                            "`reported` VARCHAR(20) NOT NULL, " +
                            "`reason` TEXT NOT NULL, " +
                            "`report-date` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                            "`status` VARCHAR(6) NOT NULL DEFAULT '" + ReportStatus.OPENED.name() + "', " +
                            "`moderator` VARCHAR(20) DEFAULT NULL, " +
                            "`server-id` VARCHAR(50) NOT NULL" +
                            ")");

            statement.execute(
                    "CREATE TABLE IF NOT EXISTS `users` (" +
                            "`username` VARCHAR(20) PRIMARY KEY, " +
                            "`passed-checks` INT, " +
                            "`failed-checks` INT, " +
                            "`last-check` DATETIME DEFAULT NULL, " +
                            "`moderator` BOOLEAN, " +
                            "`true-reports` INT, " +
                            "`false-reports` INT, " +
                            "`mute-end-date` DATETIME DEFAULT NULL, " +
                            "`mute-reason` TEXT DEFAULT NULL" +
                            ")");

            final ResultSet resultSet = statement.executeQuery("SELECT 1 FROM `users` WHERE `username` = 'System' LIMIT 1");
            if (resultSet.next()) return;

            statement.execute("INSERT INTO `users` (`username`, `passed-checks`, `failed-checks`,`moderator`," +
                    " `true-reports`, `false-reports`) VALUES ('System','0','0','1','0','0')");
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при загрузке базы данных", e);
        } finally {
            returnConnection(conn);
        }
    }
}
