package com.anm.demo;

import com.anm.core.Id;
import com.anm.core.Table;

import java.io.Serializable;

/**
 * 公司
 */
@Table("tb_company")
public class Company implements Serializable {
    @Id
    private Long id;
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
