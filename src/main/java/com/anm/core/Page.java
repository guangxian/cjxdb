package com.anm.core;

import java.util.List;

public class Page<T> {
    private final List<T> records;
    private final long total;
    private final int current;
    private final int size;

    public Page(List<T> records, long total, int current, int size) {
        this.records = records;
        this.total = total;
        this.current = current;
        this.size = size;
    }

    public List<T> getRecords() {
        return records;
    }

    public long getTotal() {
        return total;
    }

    public int getCurrent() {
        return current;
    }

    public int getSize() {
        return size;
    }
}
