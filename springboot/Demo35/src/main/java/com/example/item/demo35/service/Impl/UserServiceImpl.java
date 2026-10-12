package com.example.item.demo35.service.Impl;

import com.example.item.demo35.Dao.UserDao;
import com.example.item.demo35.config.BigDataConfig;
import com.example.item.demo35.service.UserService;
import com.example.item.demo35.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.kafka.core.KafkaTemplate;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;

    //==============================Kafka========================
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private Gson gson = new Gson();

    @Autowired
    private BigDataConfig bigDataConfig;


    private void sendUserEvent(String eventType, User user) {
        if (!bigDataConfig.isEnabled()) {
            return; // 如果大数据功能未启用，不发送 Kafka 消息
        }
        try {
            // 构建一个简单的 JSON 消息
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("timestamp", System.currentTimeMillis());
            event.put("user", user);
            String message = gson.toJson(event);
            kafkaTemplate.send("user-change", message);
            System.out.println("发送 Kafka 消息: " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //==============================Kafka========================

    @Override
    public User login(Integer number, String password) {
        return userDao.login(number,password);
    }


    @Override
    public boolean register(User user) {
        //return userDao.register(user);
        boolean result = userDao.register(user);
        System.out.println("userserviceImpl阶段，已经联通数据库");
        if (result) {
            sendUserEvent("REGISTER", user);
            System.out.println("userserviceImpl阶段，已经发送kafka消息");
        }
        return result;
    }

    @Override
    public User selectByName(String name) {
        return userDao.selectByName(name);
    }

    @Override
    public Integer selectIdByName(String name) {
        User user = userDao.selectByName(name);
        return user != null ? user.getId() : null;
    }

    @Override
    public List<User> selectAllUsers() {
        return userDao.selectAllUsers();
    }

    @Override
    public User selectById(Integer id) {
        return userDao.selectById(id);
    }

    @Override
    public boolean updateUser(User user) {
        //return userDao.updateUser(user);
        boolean result = userDao.updateUser(user);
        if (result) {
            sendUserEvent("UPDATE", user);
        }
        return result;
    }

    @Override
    public boolean deleteUser(Integer id) {
        //return userDao.deleteUser(id);
        User user = userDao.selectById(id);
        boolean result = userDao.deleteUser(id);
        if (result) {
            sendUserEvent("DELETE", user);
        }
        return result;
    }
}
