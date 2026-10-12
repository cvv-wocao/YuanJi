package com.example.item.demo35.Dao;

import com.example.item.demo35.entity.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminDao {
    Admin login(@Param("username") String username, @Param("password") String password);

    Admin selectByUsername(@Param("username") String username);

    List<Admin> selectAll();

    Admin selectById(@Param("id") Integer id);

    boolean updateAdmin(Admin admin);

    boolean deleteAdmin(@Param("id") Integer id);

    boolean createAdmin(Admin admin);
}
