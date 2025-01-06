package com.anm.core;

// QueryWrapper.java
import java.util.*;

/**
 * 查询
 * @param <T>
 */
public class QueryWrapper<T> {
    private final Map<String, Object> conditions = new HashMap<>();
    private final List<String> orderBy = new ArrayList<>();
    private final List<String> joins = new ArrayList<>();

    public QueryWrapper<T> eq(String column, Object value) {
        conditions.put(column, value);
        return this;
    }

    public QueryWrapper<T> like(String column, String value) {
        conditions.put(column + " LIKE", "%" + value + "%");
        return this;
    }

    public QueryWrapper<T> in(String column, List<?> values) {
        conditions.put(column + " IN", values);
        return this;
    }

    public QueryWrapper<T> gt(String column, Object value) {
        conditions.put(column + " >", value);
        return this;
    }

    public QueryWrapper<T> lt(String column, Object value) {
        conditions.put(column + " <", value);
        return this;
    }

    public QueryWrapper<T> orderBy(String column, String direction) {
        orderBy.add(column + " " + direction);
        return this;
    }

    public QueryWrapper<T> leftJoin(String table, String column, Object value) {
        joins.add("LEFT JOIN " + table + " ON " + table + "." + column + " = ?");
        conditions.put(table + "." + column, value);
        return this;
    }

    public Map<String, Object> getConditions() {
        return conditions;
    }

    public List<String> getOrderBy() {
        return orderBy;
    }

    public List<String> getJoins() {
        return joins;
    }
}