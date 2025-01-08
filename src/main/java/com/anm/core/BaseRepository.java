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
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        List<T> result = selectList(wrapper);

        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public Optional<T> selectOne(QueryWrapper<T> wrapper) throws SQLException, InstantiationException, IllegalAccessException {
        List<T> results = selectList(wrapper);
        if (!results.isEmpty()) {
            return Optional.of(results.get(0));
        }
        return Optional.empty();
    }

    public List<T> selectList(QueryWrapper<T> wrapper) throws SQLException, InstantiationException, IllegalAccessException {
        return executeQuery(wrapper, -1, -1);
    }

    public Page<T> selectPage(int current, int size, QueryWrapper<T> wrapper) throws SQLException, InstantiationException, IllegalAccessException {
        List<T> records = executeQuery(wrapper, current, size);
        long total = count(wrapper);

        return new Page<>(records, total, current, size);
    }

    private long count(QueryWrapper<T> wrapper) throws SQLException {
        String tableName = clazz.getAnnotation(Table.class).value();
        String alias = "t0";
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM " + tableName + " " + alias + " ");

        Map<String, String> joinAliases = new HashMap<>();
        final int[] aliasIndex = {1}; // 使用数组来解决 lambda 表达式引用的问题

        for (String condition : wrapper.getConditions().keySet()) {
            if (condition.contains(".")) {
                String[] parts = condition.split("\\.");
                String joinProperty = parts[0];
                String joinField = parts[1];

                Field joinFieldObj = Arrays.stream(clazz.getDeclaredFields())
                        .filter(f -> f.getName().equals(joinProperty) && f.getType().isAnnotationPresent(Table.class))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No such field: " + joinProperty));

                String joinTable = joinFieldObj.getType().getAnnotation(Table.class).value();
                String joinAlias = joinAliases.computeIfAbsent(joinProperty, k -> "t" + aliasIndex[0]++);
                String joinColumn = convertToSnakeCase(joinProperty) + "_id";

                sql.append("LEFT JOIN ").append(joinTable).append(" ").append(joinAlias)
                        .append(" ON ").append(alias).append(".").append(joinColumn)
                        .append(" = ").append(joinAlias).append(".id ");
            }
        }

        sql.append("WHERE ");
        List<Object> params = new ArrayList<>();
        wrapper.getConditions().forEach((condition, value) -> {
            String columnName;
            if (condition.contains(".")) {
                String[] parts = condition.split("\\.");
                String joinAlias = joinAliases.get(parts[0]);
                String joinField = parts[1];
                columnName = joinAlias + "." + convertToSnakeCase(joinField);
            } else {
                columnName = alias + "." + convertToSnakeCase(condition);
            }
            sql.append(columnName).append(" = ? AND ");
            params.add(value);
        });
        sql.delete(sql.length() - 5, sql.length());

        LOGGER.info("Executing SQL: " + sql.toString());

        try (Connection connection = SessionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
            return 0;
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

    private List<T> executeQuery(QueryWrapper<T> wrapper, int current, int size) throws SQLException, InstantiationException, IllegalAccessException {
        String tableName = clazz.getAnnotation(Table.class).value();
        String alias = "t0";
        StringBuilder sql = new StringBuilder("SELECT " + alias + ".*, ");

        Map<String, String> joinAliases = new HashMap<>();
        final int[] aliasIndex = {1}; // 使用数组来解决 lambda 表达式引用的问题

        for (String condition : wrapper.getConditions().keySet()) {
            if (condition.contains(".")) {
                String[] parts = condition.split("\\.");
                String joinProperty = parts[0];
                String joinField = parts[1];

                Field joinFieldObj = Arrays.stream(clazz.getDeclaredFields())
                        .filter(f -> f.getName().equals(joinProperty) && f.getType().isAnnotationPresent(Table.class))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No such field: " + joinProperty));

                String joinTable = joinFieldObj.getType().getAnnotation(Table.class).value();
                String joinAlias = joinAliases.computeIfAbsent(joinProperty, k -> "t" + aliasIndex[0]++);
                String joinColumn = convertToSnakeCase(joinProperty) + "_id";

                sql.append(joinAlias).append(".*, ");
                sql.append("LEFT JOIN ").append(joinTable).append(" ").append(joinAlias)
                        .append(" ON ").append(alias).append(".").append(joinColumn)
                        .append(" = ").append(joinAlias).append(".id ");
            }
        }

        sql.setLength(sql.length() - 2); // 移除最后一个逗号和空格
        sql.append(" FROM ").append(tableName).append(" ").append(alias).append(" WHERE ");

        List<Object> params = new ArrayList<>();
        wrapper.getConditions().forEach((condition, value) -> {
            String columnName;
            if (condition.contains(".")) {
                String[] parts = condition.split("\\.");
                String joinAlias = joinAliases.get(parts[0]);
                String joinField = parts[1];
                columnName = joinAlias + "." + convertToSnakeCase(joinField);
            } else {
                columnName = alias + "." + convertToSnakeCase(condition);
            }
            sql.append(columnName).append(" = ? AND ");
            params.add(value);
        });
        sql.delete(sql.length() - 5, sql.length());

        if (!wrapper.getOrderBy().isEmpty()) {
            sql.append(" ORDER BY ").append(String.join(", ", wrapper.getOrderBy()));
        }

        if (current > 0 && size > 0) {
            int offset = (current - 1) * size;
            sql.append(" LIMIT ").append(size).append(" OFFSET ").append(offset);
        }

        LOGGER.info("Executing SQL: " + sql.toString());

        try (Connection connection = SessionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            ResultSet resultSet = statement.executeQuery();
            return mapResultSetToList(resultSet, alias, joinAliases);
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

    private Field getIdField(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }
        throw new RuntimeException("No @Id field found in class " + clazz.getSimpleName());
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

    private List<T> mapResultSetToList(ResultSet resultSet, String alias, Map<String, String> joinAliases) throws SQLException, InstantiationException, IllegalAccessException {
        List<T> list = new ArrayList<>();
        while (resultSet.next()) {
            list.add(mapResultSetToEntity(resultSet, alias, joinAliases));
        }
        return list;
    }
    private T mapResultSetToEntity(ResultSet resultSet, String alias, Map<String, String> joinAliases) throws SQLException, InstantiationException, IllegalAccessException {
        T entity = clazz.newInstance();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            String columnName = convertToSnakeCase(field.getName());
            if (field.getType().isAnnotationPresent(Table.class)) {
                // 处理多对一关系
                String joinAlias = joinAliases.get(field.getName());
                if (joinAlias != null) {
                    Object foreignEntity = field.getType().newInstance();
                    mapResultSetToForeignEntity(resultSet, foreignEntity, joinAlias);
                    field.set(entity, foreignEntity);
                }
            } else {
                field.set(entity, convertValue(resultSet.getObject(alias + "." + columnName), field.getType()));
            }
        }
        return entity;
    }


    private void mapResultSetToForeignEntity(ResultSet resultSet, Object foreignEntity, String joinAlias) throws SQLException, IllegalAccessException {
        Class<?> foreignClass = foreignEntity.getClass();
        for (Field field : foreignClass.getDeclaredFields()) {
            field.setAccessible(true);
            String columnName = joinAlias + "." + convertToSnakeCase(field.getName());
            field.set(foreignEntity, convertValue(resultSet.getObject(columnName), field.getType()));
        }
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        if (targetType == Long.class || targetType == long.class) {
            return ((Number) value).longValue();
        }
        if (targetType == Integer.class || targetType == int.class) {
            return ((Number) value).intValue();
        }
        // Add other type conversions as needed
        throw new IllegalArgumentException("Cannot convert value of type " + value.getClass() + " to " + targetType);
    }

    private void loadManyToOneRelations(T entity) throws SQLException, IllegalAccessException, InstantiationException {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getType().isAnnotationPresent(Table.class)) {
                field.setAccessible(true);
                Object foreignEntity = field.get(entity);
                if (foreignEntity != null) {
                    Field foreignKeyField = getIdField(foreignEntity.getClass());
                    foreignKeyField.setAccessible(true);
                    Long foreignKeyId = (Long) foreignKeyField.get(foreignEntity);
                    BaseRepository<?> foreignRepository = getRepositoryInstance(foreignEntity.getClass());
                    Optional<?> loadedForeignEntity = foreignRepository.selectById(foreignKeyId);
                    loadedForeignEntity.ifPresent(foreign -> {
                        try {
                            field.set(entity, foreign);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        }
    }

    private BaseRepository<?> getRepositoryInstance(Class<?> clazz) {
        try {
            String repositoryClassName = clazz.getPackage().getName() + "." + clazz.getSimpleName() + "Repository";
            return (BaseRepository<?>) Class.forName(repositoryClassName).newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create repository instance for " + clazz.getSimpleName(), e);
        }
    }

}