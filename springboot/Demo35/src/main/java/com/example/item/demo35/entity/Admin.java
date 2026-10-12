package com.example.item.demo35.entity;

import lombok.Data;

@Data
public class Admin {

    private Integer id;
    private String username;
    private String password;
    private String email;
    private String phone;
    private Integer level;
    private String avatar;
}
