package com.anm.core;

// TransactionManager.java
import java.sql.Connection;
import java.sql.SQLException;

public class TransactionManager {

    public static void beginTransaction() throws SQLException {
        Connection connection = SessionManager.getConnection();
        connection.setAutoCommit(false);
    }

    public static void commit() throws SQLException {
        Connection connection = SessionManager.getConnection();
        connection.commit();
        SessionManager.close();
    }

    public static void rollback() throws SQLException {
        Connection connection = SessionManager.getConnection();
        connection.rollback();
        SessionManager.close();
    }
}
