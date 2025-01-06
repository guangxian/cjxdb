package com.anm.core;

// SessionManager.java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

public class SessionManager {
    private static final String URL = "jdbc:mysql://gz-cynosdbmysql-grp-mh2itlkp.sql.tencentcdb.com:29335/aaaaa?nullCatalogMeansCurrent=true&useUnicode=true&serverTimezone=GMT%2b8&characterEncoding=utf-8&useSSL=true";
    private static final String USER = "root";
    private static final String PASSWORD = "123456mysql";
    private static final Logger LOGGER = Logger.getLogger(SessionManager.class.getName());

    private static ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    public static Connection getConnection() throws SQLException {
        if (connectionHolder.get() == null || connectionHolder.get().isClosed()) {
            Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
            connectionHolder.set(connection);
            LOGGER.info("Connection established: " + connection.toString());
        }
        return connectionHolder.get();
    }

    public static void close() throws SQLException {
        Connection connection = connectionHolder.get();
        if (connection != null && !connection.isClosed()) {
            LOGGER.info("Closing connection: " + connection.toString());
            connection.close();
            connectionHolder.remove();
        }
    }
}
