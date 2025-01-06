package ru.flamexander.db.interaction.lesson;

import java.sql.SQLException;

public class MockChatServer {
    public static void main(String[] args) {
        DataSource dataSource = null;
        try {
            System.out.println("Сервер чата запущен");
            //dataSource = new DataSource("jdbc:h2:file:./db;MODE=PostgreSQL");
            dataSource = new DataSource("jdbc:postgresql://localhost:5432/otus", "java", "java");
            dataSource.connect();
            UsersDao usersDao = new UsersDao(dataSource);
            DbMigrator.runScript();
            System.out.println(usersDao.getAllUsers());

            AbstractRepository<User> usersRepository = new AbstractRepository<>(dataSource, User.class);
            usersRepository.save(new User(null, "Login1", "pass1", "nickname1"));
            usersRepository.save(new User(null, "Login2", "pass2", "nickname2"));
            usersRepository.update(new User(1L, "Login_new1", "pass_new1", "nickname_new1"));
            System.out.println(usersRepository.findById(1));

            for (User user: usersRepository.findByAll()) {
                System.out.println(user);
            }

            usersRepository.delete(new User(1L, "Login_new1", "pass_new1", "nickname_new1"));

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (dataSource != null) {
                dataSource.close();
            }
            System.out.println("Сервер чата завершил свою работу");
        }
    }
}
