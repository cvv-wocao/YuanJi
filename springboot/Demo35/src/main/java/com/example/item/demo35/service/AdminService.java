package com.example.item.demo35.service;

import com.example.item.demo35.entity.Admin;
import com.example.item.demo35.entity.Order;
import com.example.item.demo35.entity.OrderItem;

import java.util.List;
import java.util.Map;

public interface AdminService {

    List<Order> getAllOrders(Map<String, Object> params);

    Long countAllOrders(Map<String, Object> params);

    List<Order> getOrdersByUserId(Map<String, Object> params);

    Order getOrderDetail(String orderNo);

    List<OrderItem> getOrderItems(Integer orderId);

    boolean updateOrderStatus(String orderNo, Integer status, String reason);

    Map<String, Object> getOrderStatistics(String startDate, String endDate);

    Admin login(String username, String password);

    Admin selectByUsername(String username);

    List<Admin> selectAll();

    Admin selectById(Integer id);

    boolean updateAdmin(Admin admin);

    boolean deleteAdmin(Integer id);

    boolean createAdmin(Admin admin);

    Long countOrdersByUserId(Integer userId, Integer status);
}
