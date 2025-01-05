package com.anm.demo;

import com.anm.core.Id;
import com.anm.core.Table;

import java.io.Serializable;

@Table("tb_user")
public class User implements Serializable {
    @Id
    private Long id;
    private String username;
    private String password;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
