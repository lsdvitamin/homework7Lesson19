package ru.flamexander.db.interaction.lesson;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AbstractRepository<T> {
    ResultSet rs;
    private DataSource dataSource;
    private PreparedStatement psInsert;
    private PreparedStatement psUpdateById;
    private PreparedStatement psDeleteById;
    private PreparedStatement psfindById;
    private PreparedStatement psfindAll;
    private List<Field> cachedFields;
    private List<Field> cachedFieldsUpdateById;
    private List<Field> cachedFieldsDeleteById;
    private List<Field> cachedFieldsFindById;
    private List<Field> cachedFieldsFindAll;

    public AbstractRepository(DataSource dataSource, Class<T> cls) {
        this.dataSource = dataSource;
        this.prepareInsert(cls);
        this.prepareUpdate(cls);
        this.prepareDelete(cls);
        this.prepareFindById(cls);
        this.prepareFindAll(cls);
    }


    public void save(T entity) {
        try {
            for (int i = 0; i < cachedFields.size(); i++) {
                psInsert.setObject(i + 1, cachedFields.get(i).get(entity));
            }
            psInsert.executeUpdate();
        } catch (Exception e) {
            throw new ORMException("Что-то пошло не так при сохранении: " + entity);
        }
    }

    public void update(T entity) throws SQLException {
        try {
            for (int i = 0; i < cachedFieldsUpdateById.size(); i++) {
                psUpdateById.setObject(cachedFieldsUpdateById.size() - i, cachedFieldsUpdateById.get(i).get(entity));
            }
            psUpdateById.executeUpdate();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new ORMException("Что-то пошло не так при обновлении записи с id = ");
        }
    }

    public void delete(T entity) throws SQLException {
        try {
            psDeleteById.setObject(1, cachedFieldsDeleteById.get(0).get(entity));
            psDeleteById.executeUpdate();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new ORMException("Что-то пошло не так при обновлении записи с id = ");
        }
    }

    public T findById(int id) throws SQLException {
        User u = null;
        try {
            psfindById.setLong(1, id);
            rs = psfindById.executeQuery();
            while (rs.next()) {
                u = new User(rs.getLong("id"), rs.getString("login"), rs.getString("pass"), rs.getString("nickname"));
            }
        } catch (Exception e) {
            throw new ORMException("Что-то пошло не так при поиске записи id =: " + id);
        }

        return (T) u;
    }

    public List<T> findByAll() throws SQLException {
        User u = null;
        List<User> users = new ArrayList<>();
        try {
            rs = psfindAll.executeQuery();
            while (rs.next()) {
                users.add(new User(rs.getLong("id"), rs.getString("login"), rs.getString("pass"), rs.getString("nickname")));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new ORMException("Что-то пошло не так при выборке всех записей из users");
        }

        return (List<T>) users;
    }


    private void prepareInsert(Class cls) {
        if (!cls.isAnnotationPresent(RepositoryTable.class)) {
            throw new ORMException("Класс не предназначен для создания репозитория, не хватает аннотации @RepositoryTable");
        }
        String tableName = ((RepositoryTable) cls.getAnnotation(RepositoryTable.class)).title();

        StringBuilder query = new StringBuilder("insert into ");
        query.append(tableName).append(" (");

        cachedFields = Arrays.stream(cls.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(RepositoryField.class))
                //.filter(f -> ((RepositoryField) f.isAnnotationPresent(RepositoryField.class)).name()
                .filter(f -> !f.isAnnotationPresent(RepositoryIdField.class))
                .collect(Collectors.toList());
        for (Field f : cachedFields) { // TODO заменить на использование геттеров
            f.setAccessible(true);
        }
        for (Field f : cachedFields) {
            query.append(getColumnName(f)).append(", ");
        }

        query.setLength(query.length() - 2);
        query.append(") values (");

        for (Field f : cachedFields) {
            query.append("?, ");
        }
        query.setLength(query.length() - 2);
        query.append(");");

        try {
            psInsert = dataSource.getConnection().prepareStatement(query.toString());
        } catch (SQLException e) {
            throw new ORMException("Не удалось проинициализировать репозиторий для класса " +
                    ((RepositoryTable) cls.getAnnotation(RepositoryTable.class)).title());
        }
    }

    private void prepareUpdate(Class cls) {
        if (!cls.isAnnotationPresent(RepositoryTable.class)) {
            throw new ORMException("Класс не предназначен для создания репозитория, не хватает аннотации @RepositoryTable");
        }
        String tableName = ((RepositoryTable) cls.getAnnotation(RepositoryTable.class)).title();
        StringBuilder query = new StringBuilder("update ");
        query.append(tableName).append(" set ");
        cachedFieldsUpdateById = Arrays.stream(cls.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(RepositoryField.class))
                .filter(f -> !f.isAnnotationPresent(RepositoryIdField.class))
                .collect(Collectors.toList());
        for (Field f : cachedFieldsUpdateById) { // TODO заменить на использование геттеров
            f.setAccessible(true);
        }
        for (Field f : cachedFieldsUpdateById) {
            query.append(getColumnName(f)).append(" = ?, ");
        }

        query.setLength(query.length() - 2);
        query.append(" where ");
        cachedFieldsUpdateById = Arrays.stream(cls.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(RepositoryIdField.class))
                .collect(Collectors.toList());
        query.append(cachedFieldsUpdateById.get(0).getName());
        query.append(" = ?;");
        cachedFieldsUpdateById = Arrays.stream(cls.getDeclaredFields())
                .collect(Collectors.toList());
        try {
            psUpdateById = dataSource.getConnection().prepareStatement(query.toString());
        } catch (SQLException e) {
            throw new ORMException("Не удалось проинициализировать репозиторий для класса " +
                    ((RepositoryTable) cls.getAnnotation(RepositoryTable.class)).title());
        }
    }

    private void prepareDelete(Class cls) {
        if (!cls.isAnnotationPresent(RepositoryTable.class)) {
            throw new ORMException("Класс не предназначен для создания репозитория, не хватает аннотации @RepositoryTable");
        }
        String tableName = ((RepositoryTable) cls.getAnnotation(RepositoryTable.class)).title();
        StringBuilder query = new StringBuilder("delete from ");
        query.append(tableName).append(" where ");
        cachedFieldsDeleteById = Arrays.stream(cls.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(RepositoryIdField.class))
                .collect(Collectors.toList());
        query.append(cachedFieldsDeleteById.get(0).getName());
        query.append(" = ?;");
        cachedFieldsDeleteById = Arrays.stream(cls.getDeclaredFields())
                .collect(Collectors.toList());
        try {
            psDeleteById = dataSource.getConnection().prepareStatement(query.toString());
        } catch (SQLException e) {
            throw new ORMException("Не удалось проинициализировать репозиторий для класса " +
                    ((RepositoryTable) cls.getAnnotation(RepositoryTable.class)).title());
        }
    }

    private void prepareFindById(Class cls) {
        if (!cls.isAnnotationPresent(RepositoryTable.class)) {
            throw new ORMException("Класс не предназначен для создания репозитория, не хватает аннотации @RepositoryTable");
        }
        String tableName = ((RepositoryTable) cls.getAnnotation(RepositoryTable.class)).title();
        StringBuilder query = new StringBuilder("select ");
        cachedFieldsFindById = Arrays.stream(cls.getDeclaredFields())
                .collect(Collectors.toList());
        for (Field f : cachedFieldsFindById) { // TODO заменить на использование геттеров
            f.setAccessible(true);
        }
        for (Field f : cachedFieldsFindById) {
            query.append(getColumnName(f)).append(", ");
        }

        query.setLength(query.length() - 2);
        query.append(" from ").append(tableName).append(" where ");

        cachedFieldsFindById = Arrays.stream(cls.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(RepositoryIdField.class))
                .collect(Collectors.toList());

        query.append(cachedFieldsFindById.get(0).getName());
        query.append(" = ?;");

        try {
            psfindById = dataSource.getConnection().prepareStatement(query.toString());
        } catch (SQLException e) {
            throw new ORMException("Не удалось проинициализировать репозиторий для класса " +
                    ((RepositoryTable) cls.getAnnotation(RepositoryTable.class)).title());
        }
    }


    private void prepareFindAll(Class cls) {
        if (!cls.isAnnotationPresent(RepositoryTable.class)) {
            throw new ORMException("Класс не предназначен для создания репозитория, не хватает аннотации @RepositoryTable");
        }
        String tableName = ((RepositoryTable) cls.getAnnotation(RepositoryTable.class)).title();
        StringBuilder query = new StringBuilder("select ");
        cachedFieldsFindAll = Arrays.stream(cls.getDeclaredFields())
                .collect(Collectors.toList());
        for (Field f : cachedFieldsFindAll) { // TODO заменить на использование геттеров
            f.setAccessible(true);
        }
        for (Field f : cachedFieldsFindAll) {
            query.append(getColumnName(f)).append(", ");
        }

        query.setLength(query.length() - 2);
        query.append(" from ").append(tableName);

        try {
            psfindAll = dataSource.getConnection().prepareStatement(query.toString());
        } catch (SQLException e) {
            throw new ORMException("Не удалось проинициализировать репозиторий для класса " +
                    ((RepositoryTable) cls.getAnnotation(RepositoryTable.class)).title());
        }
    }

    public String getColumnName(Field field) {
        if (field.getAnnotation(RepositoryIdField.class) instanceof RepositoryIdField)
            return field.getAnnotation(RepositoryIdField.class).name();
        if (!field.getAnnotation(RepositoryField.class).name().isEmpty())
            return field.getAnnotation(RepositoryField.class).name();
        else
            return field.getName();
    }

}
