package ru.flamexander.db.interaction.lesson;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DataSource {
    String url;
    String login;
    String password;
    private Connection connection;
    private Statement statement;

    public Connection getConnection() {
        return connection;
    }

    public Statement getStatement() {
        return statement;
    }

    public DataSource(String url, String login, String password) {
        this.url = url;
        this.login = login;
        this.password = password;
    }

    public void connect() throws SQLException {
        connection = DriverManager.getConnection(url, login, password);
        statement = connection.createStatement();
        System.out.println("Установлено соединение с БД: " + url);
    }

    public void close() {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        System.out.println("От БД отключились");
    }
}
