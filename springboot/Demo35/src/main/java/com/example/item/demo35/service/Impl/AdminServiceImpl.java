package com.example.item.demo35.service.Impl;

import com.example.item.demo35.Dao.AdminDao;
import com.example.item.demo35.Dao.OrderDao;
import com.example.item.demo35.Dao.OrderItemDao;
import com.example.item.demo35.Dao.ProductDao;
import com.example.item.demo35.entity.Admin;
import com.example.item.demo35.entity.Order;
import com.example.item.demo35.entity.OrderItem;
import com.example.item.demo35.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AdminServiceImpl implements AdminService {

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private OrderItemDao orderItemDao;

    @Autowired
    private ProductDao productDao;

    @Autowired
    private AdminDao adminDao;

    //=================================================================================
    @Override
    public List<Order> getAllOrders(Map<String, Object> params) {
        // 计算分页偏移量
        Integer page = (Integer) params.get("page");
        Integer pageSize = (Integer) params.get("pageSize");
        Integer offset = (page - 1) * pageSize;

        return orderDao.selectAllOrders(params, offset, pageSize);
    }

    @Override
    public Long countAllOrders(Map<String, Object> params) {
        return orderDao.countAllOrders(params);
    }

    @Override
    public List<Order> getOrdersByUserId(Map<String, Object> params) {
        Integer page = (Integer) params.get("page");
        Integer pageSize = (Integer) params.get("pageSize");
        Integer offset = (page - 1) * pageSize;

        return orderDao.selectOrdersByUserId(params, offset, pageSize);
    }

    @Override
    public Long countOrdersByUserId(Integer userId, Integer status) {
        return orderDao.countOrdersByUserId(userId, status);
    }

    @Override
    public Order getOrderDetail(String orderNo) {
        return orderDao.selectByOrderNo(orderNo);
    }

    @Override
    public List<OrderItem> getOrderItems(Integer orderId) {
        return orderItemDao.selectByOrderId(orderId);
    }

    @Override
    @Transactional
    public boolean updateOrderStatus(String orderNo, Integer status, String reason) {
        Order order = orderDao.selectByOrderNo(orderNo);
        if (order == null) {
            return false;
        }

        Integer oldStatus = order.getStatus();

        // 更新订单状态
        order.setStatus(status);

        // 根据状态更新相应的时间字段
        if (status == 2) { // 已发货
            order.setShippingTime(new Date());
        } else if (status == 3) { // 已完成
            order.setReceiveTime(new Date());
        } else if (status == 4) { // 已取消
            order.setCancelReason(reason);
        } else if (status == 5) { // 退款中
            order.setCancelReason(reason);
        }

        boolean result = orderDao.update(order);

        if (result) {
            // 可以在这里记录订单操作日志
            System.out.println("订单状态更新：订单号=" + orderNo +
                    "，原状态=" + oldStatus +
                    "，新状态=" + status +
                    "，原因=" + reason);
        }

        return result;
    }

    @Override
    public Map<String, Object> getOrderStatistics(String startDate, String endDate) {
        Map<String, Object> statistics = new HashMap<>();

        try {
            // 解析日期
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date start = null;
            Date end = null;

            if (startDate != null && !startDate.isEmpty()) {
                start = sdf.parse(startDate + " 00:00:00");
            }
            if (endDate != null && !endDate.isEmpty()) {
                end = sdf.parse(endDate + " 23:59:59");
            }

            // 总订单数
            Long totalOrders = orderDao.countTotalOrders(start, end);
            statistics.put("totalOrders", totalOrders);

            // 总销售额
            BigDecimal totalAmount = orderDao.sumTotalAmount(start, end);
            statistics.put("totalAmount", totalAmount != null ? totalAmount : BigDecimal.ZERO);

            // 各状态订单统计
            List<Map<String, Object>> statusStats = orderDao.countOrdersGroupByStatusWithDate(start, end);
            Map<Integer, Long> statusCount = new HashMap<>();
            for (Map<String, Object> stat : statusStats) {
                Object statusObj = stat.get("status");
                Integer status = statusObj instanceof Number ? ((Number) statusObj).intValue() : (Integer) statusObj;
                Object countObj = stat.get("count");
                Long count = countObj instanceof Number ? ((Number) countObj).longValue() : (Long) countObj;
                statusCount.put(status, count);
            }
            statistics.put("statusCount", statusCount);

            // 待发货订单数（用于快速发货提醒）
            Long pendingShipCount = statusCount.getOrDefault(1, 0L);
            statistics.put("pendingShipCount", pendingShipCount);

            // 退款中订单数
            Long refundingCount = statusCount.getOrDefault(5, 0L);
            statistics.put("refundingCount", refundingCount);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return statistics;
    }
    //=================================================================================

    @Override
    public Admin login(String username, String password) {
        return adminDao.login(username, password);
    }

    @Override
    public List<Admin> selectAll() {
        return adminDao.selectAll();
    }

    @Override
    public Admin selectById(Integer id) {
        return adminDao.selectById(id);
    }

    @Override
    public boolean updateAdmin(Admin admin) {
        return adminDao.updateAdmin(admin);
    }

    @Override
    public boolean deleteAdmin(Integer id) {
        return adminDao.deleteAdmin(id);
    }

    @Override
    public Admin selectByUsername(String username) {
        return adminDao.selectByUsername(username);
    }

    @Override
    public boolean createAdmin(Admin admin) {
        return adminDao.createAdmin(admin);
    }
}
