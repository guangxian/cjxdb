package com.anm.core;

// BaseRepository.java
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * 核心支持
 * @param <T>
 */
public abstract class BaseRepository<T> {
    private static final Logger LOGGER = Logger.getLogger(BaseRepository.class.getName());
    private final Class<T> clazz;

    @SuppressWarnings("unchecked")
    protected BaseRepository() {
        this.clazz = (Class<T>) ((java.lang.reflect.ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public T insert(T entity) throws SQLException, IllegalAccessException {
        String tableName = clazz.getAnnotation(Table.class).value();
        StringBuilder sql = new StringBuilder("INSERT INTO " + tableName + " (");
        StringBuilder values = new StringBuilder("VALUES (");
        List<Object> params = new ArrayList<>();
        Field idField = null;

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.getAnnotation(Id.class) == null) {
                String columnName = convertToSnakeCase(field.getName());
                if (field.getType().isAnnotationPresent(Table.class)) {
                    columnName += "_id";
                    Field foreignKeyField = getIdField(field.getType());
                    foreignKeyField.setAccessible(true);
                    params.add(foreignKeyField.get(field.get(entity)));
                } else {
                    params.add(field.get(entity));
                }
                sql.append(columnName).append(", ");
                values.append("?, ");
            } else {
                idField = field;
            }
        }

        sql.delete(sql.length() - 2, sql.length()).append(") ");
        values.delete(values.length() - 2, values.length()).append(")");
        sql.append(values);

        LOGGER.info("Executing SQL: " + sql.toString());

        try (Connection connection = SessionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            statement.executeUpdate();
            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next() && idField != null) {
                idField.setAccessible(true);
                Object generatedKey = generatedKeys.getObject(1);
                if (generatedKey instanceof BigInteger) {
                    idField.set(entity, ((BigInteger) generatedKey).longValue());
                } else if (generatedKey instanceof Number) {
                    idField.set(entity, ((Number) generatedKey).longValue());
                } else {
                    idField.set(entity, generatedKey);
                }
            }
        } finally {
            SessionManager.close();
        }
        return entity;
    }

    private String convertToSnakeCase(String camelCase) {
        StringBuilder result = new StringBuilder();
        result.append(Character.toLowerCase(camelCase.charAt(0)));
        for (int i = 1; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                result.append('_').append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private Field getIdField(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }
        throw new RuntimeException("No @Id field found in class " + clazz.getSimpleName());
    }

    public void deleteById(Long id) throws SQLException {
        String tableName = clazz.getAnnotation(Table.class).value();
        String idColumn = getIdColumn();
        String sql = "DELETE FROM " + tableName + " WHERE " + idColumn + " = ?";

        LOGGER.info("Executing SQL: " + sql);

        try (Connection connection = SessionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } finally {
            SessionManager.close();
        }
    }

    public void update(T entity) throws SQLException, IllegalAccessException {
        String tableName = clazz.getAnnotation(Table.class).value();
        StringBuilder sql = new StringBuilder("UPDATE " + tableName + " SET ");
        List<Object> params = new ArrayList<>();
        Object idValue = null;

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.getAnnotation(Id.class) == null) {
                sql.append(field.getName()).append(" = ?, ");
                params.add(field.get(entity));
            } else {
                idValue = field.get(entity);
            }
        }

        sql.delete(sql.length() - 2, sql.length());
        sql.append(" WHERE ").append(getIdColumn()).append(" = ?");
        params.add(idValue);

        LOGGER.info("Executing SQL: " + sql.toString());

        try (Connection connection = SessionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; params != null && i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            statement.executeUpdate();
        } finally {
            SessionManager.close();
        }
    }

    public Optional<T> selectById(Long id) throws SQLException, InstantiationException, IllegalAccessException {
        String tableName = clazz.getAnnotation(Table.class).value();
        String idColumn = getIdColumn();
        String sql = "SELECT * FROM " + tableName + " WHERE " + idColumn + " = ?";

        LOGGER.info("Executing SQL: " + sql);

        try (Connection connection = SessionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return Optional.of(mapResultSetToEntity(resultSet));
            }
            return Optional.empty();
        } finally {
            SessionManager.close();
        }
    }

    public Optional<T> selectOne(QueryWrapper<T> wrapper) throws SQLException, InstantiationException, IllegalAccessException {
        String tableName = clazz.getAnnotation(Table.class).value();
        StringBuilder sql = new StringBuilder("SELECT * FROM " + tableName + " ");
        List<Object> params = new ArrayList<>();
        buildJoins(wrapper, sql);
        buildConditions(wrapper, sql, params);
        sql.append(" LIMIT 1");

        LOGGER.info("Executing SQL: " + sql.toString());

        try (Connection connection = SessionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; params != null && i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return Optional.of(mapResultSetToEntity(resultSet));
            }
            return Optional.empty();
        } finally {
            SessionManager.close();
        }
    }

    public List<T> selectList(QueryWrapper<T> wrapper) throws SQLException, InstantiationException, IllegalAccessException {
        String tableName = clazz.getAnnotation(Table.class).value();
        StringBuilder sql = new StringBuilder("SELECT * FROM " + tableName + " ");
        List<Object> params = new ArrayList<>();
        buildJoins(wrapper, sql);
        buildConditions(wrapper, sql, params);

        LOGGER.info("Executing SQL: " + sql.toString());

        try (Connection connection = SessionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; params != null && i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            ResultSet resultSet = statement.executeQuery();
            return mapResultSetToList(resultSet);
        } finally {
            SessionManager.close();
        }
    }

    public Page<T> selectPage(int current, int size, QueryWrapper<T> wrapper) throws SQLException, InstantiationException, IllegalAccessException {
        String tableName = clazz.getAnnotation(Table.class).value();
        int offset = (current - 1) * size;

        StringBuilder sql = new StringBuilder("SELECT * FROM " + tableName + " ");
        List<Object> params = new ArrayList<>();
        buildJoins(wrapper, sql);
        buildConditions(wrapper, sql, params);
        sql.append(" LIMIT ").append(size).append(" OFFSET ").append(offset);

        LOGGER.info("Executing SQL: " + sql.toString());

        try (Connection connection = SessionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; params != null && i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            ResultSet resultSet = statement.executeQuery();
            List<T> records = mapResultSetToList(resultSet);
            long total = count(wrapper);
            return new Page<>(records, total, current, size);
        } finally {
            SessionManager.close();
        }
    }

    public long count(QueryWrapper<T> wrapper) throws SQLException {
        String tableName = clazz.getAnnotation(Table.class).value();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM " + tableName + " ");
        List<Object> params = new ArrayList<>();
        buildJoins(wrapper, sql);
        buildConditions(wrapper, sql, params);

        LOGGER.info("Executing SQL: " + sql.toString());

        try (Connection connection = SessionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; params != null && i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
            return 0;
        } finally {
            SessionManager.close();
        }
    }

    public boolean exists(QueryWrapper<T> wrapper) throws SQLException {
        String tableName = clazz.getAnnotation(Table.class).value();
        StringBuilder sql = new StringBuilder("SELECT 1 FROM " + tableName + " ");
        List<Object> params = new ArrayList<>();
        buildJoins(wrapper, sql);
        buildConditions(wrapper, sql, params);
        sql.append(" LIMIT 1");

        LOGGER.info("Executing SQL: " + sql.toString());

        try (Connection connection = SessionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; params != null && i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } finally {
            SessionManager.close();
        }
    }

    private void buildConditions(QueryWrapper<T> wrapper, StringBuilder sql, List<Object> params) {
        wrapper.getConditions().forEach((key, value) -> {
            sql.append(key).append(" = ? AND ");
            params.add(value);
        });
        sql.delete(sql.length() - 5, sql.length());
        if (!wrapper.getOrderBy().isEmpty()) {
            sql.append(" ORDER BY ").append(String.join(", ", wrapper.getOrderBy()));
        }
    }

    private void buildJoins(QueryWrapper<T> wrapper, StringBuilder sql) {
        wrapper.getJoins().forEach(join -> sql.append(join).append(" "));
    }

    private String getIdColumn() {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getAnnotation(Id.class) != null) {
                return field.getName();
            }
        }
        throw new RuntimeException("No @Id field found in class " + clazz.getSimpleName());
    }

    private T mapResultSetToEntity(ResultSet resultSet) throws SQLException, InstantiationException, IllegalAccessException {
        T entity = clazz.newInstance();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            field.set(entity, resultSet.getObject(field.getName()));
        }
        return entity;
    }

    private List<T> mapResultSetToList(ResultSet resultSet) throws SQLException, InstantiationException, IllegalAccessException {
        List<T> list = new ArrayList<>();
        while (resultSet.next()) {
            list.add(mapResultSetToEntity(resultSet));
        }
        return list;
    }
}