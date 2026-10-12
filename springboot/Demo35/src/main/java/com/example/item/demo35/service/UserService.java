package com.example.item.demo35.service;

import com.example.item.demo35.entity.User;

import java.util.List;

public interface UserService {
    User login(Integer number, String password);

    boolean register(User user);

    User selectByName(String name);

    Integer selectIdByName(String name);

    List<User> selectAllUsers();

    User selectById(Integer id);

    boolean updateUser(User user);

    boolean deleteUser(Integer id);
}
