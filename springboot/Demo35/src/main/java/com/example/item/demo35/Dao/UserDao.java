package com.example.item.demo35.Dao;

import com.example.item.demo35.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserDao {
    User login(@Param("number") Integer number, @Param("password") String password);

    boolean register(User user);

    User selectByName(@Param("name") String name);

    List<User> selectAllUsers();

    User selectById(@Param("id") Integer id);

    boolean updateUser(User user);

    boolean deleteUser(@Param("id") Integer id);

    Integer countUsers();
}
