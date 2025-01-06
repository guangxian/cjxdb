package com.anm.demo;

import com.anm.core.Id;
import com.anm.core.Table;

import java.io.Serializable;

@Table("tb_dept")
public class Dept implements Serializable {
    @Id
    private Long id;
    private String name;
    private Company company;

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

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }
}
