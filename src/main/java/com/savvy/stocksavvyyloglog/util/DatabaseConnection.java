package com.savvy.stocksavvyyloglog.util;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DatabaseConnection {
    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    static {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(".env"));

            String host = props.getProperty("DB_HOST");
            String port = props.getProperty("DB_PORT");
            String dbName = props.getProperty("DB_NAME");
            USER = props.getProperty("DB_USER");
            PASSWORD = props.getProperty("DB_PASSWORD");

            if (host == null || port == null || dbName == null || USER == null || PASSWORD == null) {
                throw new IllegalStateException(".env file is missing required values: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD");
            }

            URL = "jdbc:mysql://" + host + ":" + port + "/" + dbName;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load .env file", e);
        }
    }

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}